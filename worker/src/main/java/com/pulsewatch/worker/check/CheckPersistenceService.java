package com.pulsewatch.worker.check;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.pulsewatch.common.domain.CheckError;
import com.pulsewatch.common.domain.CheckResult;
import com.pulsewatch.common.domain.Incident;
import com.pulsewatch.common.domain.Monitor;
import com.pulsewatch.common.domain.MonitorStatus;
import com.pulsewatch.common.messaging.CheckTask;
import com.pulsewatch.persistence.repository.CheckResultRepository;
import com.pulsewatch.persistence.repository.IncidentRepository;
import com.pulsewatch.persistence.repository.MonitorRepository;

import jakarta.transaction.Transactional;

@Service
public class CheckPersistenceService {

    private static final int FAILURE_THRESHOLD = 3;

    private final MonitorRepository monitorRepository;
    private final CheckResultRepository checkResultRepository;
    private final IncidentRepository incidentRepository;

    public CheckPersistenceService(
            MonitorRepository monitorRepository,
            IncidentRepository incidentRepository,
            CheckResultRepository checkResultRepository) {
        
        this.incidentRepository = incidentRepository;
        this.monitorRepository = monitorRepository;
        this.checkResultRepository = checkResultRepository;
    }

    //@Transactional -> Everything inside this function must be atomic
    @Transactional
    public void recordResult(
                CheckTask task,
                Instant checkedAt,
                Integer statusCode,
                long latencyMs,
                CheckError error) {


        if (checkResultRepository.existsByTaskId(task.taskId())){
            return;
        }

        Monitor monitor = monitorRepository
                .findById(task.monitorId())
                .orElseThrow(() -> 
                              new IllegalStateException(
                                    "Monitor not found: " + task.monitorId()
                              ));
        
        MonitorStatus previousStatus = monitor.getStatus();

        CheckResult result = new CheckResult(task.taskId(), monitor, checkedAt, statusCode, latencyMs, error);
        
        checkResultRepository.save(result);

        boolean succesfull = error == null && isHealthyStatus(statusCode);

        if (succesfull) {
            monitor.recordSucess();

            if (previousStatus == MonitorStatus.DOWN) {
                incidentRepository
                        .findFirstByMonitorAndEndedAtIsNullOrderByStartedAtDesc(monitor)
                        .ifPresent(incident -> {
                            incident.resolve(checkedAt);
                            incidentRepository.save(incident);
                        });
            }
        } else {
            monitor.recordFailure(FAILURE_THRESHOLD);

            boolean justWentDown = 
                    previousStatus != MonitorStatus.DOWN
                    && monitor.getStatus() == MonitorStatus.DOWN;

            if (justWentDown) {
                boolean openIncidentExists = 
                        incidentRepository
                                .findFirstByMonitorAndEndedAtIsNullOrderByStartedAtDesc(monitor)
                                .isPresent();

                if (!openIncidentExists) {

                    Incident incident = new Incident(monitor, checkedAt);
                    incidentRepository.save(incident);
                }
            }
        }

        monitorRepository.save(monitor);
    }

    /**
     * Decides if the current status code is healthy
     * @param statusCode the code returned by the request
     * @return boolean value
     */
    private boolean isHealthyStatus(Integer statusCode) {

        return statusCode != null && statusCode >= 200 && statusCode < 400;
    }
    
}