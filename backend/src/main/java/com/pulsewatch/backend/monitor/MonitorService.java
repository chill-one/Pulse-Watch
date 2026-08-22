package com.pulsewatch.backend.monitor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;

import com.pulsewatch.common.domain.CheckResult;
import com.pulsewatch.common.domain.Incident;
import com.pulsewatch.common.domain.Monitor;
import com.pulsewatch.common.domain.MonitorStatus;
import com.pulsewatch.persistence.repository.CheckResultRepository;
import com.pulsewatch.persistence.repository.IncidentRepository;
import com.pulsewatch.persistence.repository.MonitorRepository;

import jakarta.transaction.Transactional;

import org.springframework.web.bind.annotation.RequestParam;



//@Service -> This class contains application/business logic and should be managed by spring
@Service
public class MonitorService {

    private final MonitorRepository monitorRepository;
    private final CheckResultRepository checkResultRepository;
    private final IncidentRepository incidentRepository;

    public MonitorService(MonitorRepository monitorRepository, CheckResultRepository checkResultRepository, IncidentRepository incidentRepository) {
        this.monitorRepository = monitorRepository;
        this.checkResultRepository = checkResultRepository;
        this.incidentRepository = incidentRepository;
    }


    /**
     * The actual monitor obj is created here
     * @param request The message from user
     * @return The entity else null
     */
    public Monitor createMonitor(CreateMonitorRequest request){
        
        Instant now = Instant.now();

        Monitor monitor = new Monitor(
                request.name(),
                request.url(),
                request.checkIntervalSeconds(),
                request.timeoutSeconds(),
                now.plusSeconds(request.checkIntervalSeconds()),
                MonitorStatus.PENDING,
                0,
                now
        );

        return monitorRepository.save(monitor);
    }

    /**
     * The JpaRepository already supplies findAll() and find__() 
     * @return All the Monitors in the Systems
     */
    public List<Monitor> getAllMointors() {
        return monitorRepository.findAll();
    }

    /**
     * Even if Monitor does not exist we can return an empty Optional which is better than null
     * @param id The id associated with the Monitor
     * @return All the Mointor listed under this id
     */
    public Optional<Monitor> getMointor(UUID id) {
        return monitorRepository.findById(id);
    }


    public Optional<List<CheckResult>> getRecentChecks(UUID monitorId, int limit) {

        Optional<Monitor> monitorOptional =
                monitorRepository.findById(monitorId);

        if (monitorOptional.isEmpty()) {
            return Optional.empty();
        }

        int safeLimit = Math.min(Math.max(limit, 1), 100);

        List<CheckResult> results =
                checkResultRepository
                        .findByMonitorOrderByCheckedAtDesc(
                                monitorOptional.get(),
                                PageRequest.of(0, safeLimit)
                        );

        return Optional.of(results);
    }


    public Optional<List<Incident>> getRecentIncidents(UUID monitorId, int limit) {

        Optional<Monitor> monitorOptional =
                monitorRepository.findById(monitorId);

        if (monitorOptional.isEmpty()) {
            return Optional.empty();
        }

        int safeLimit = Math.min(Math.max(limit, 1), 100);

        List<Incident> incidents =
                incidentRepository
                        .findByMonitorOrderByStartedAtDesc(
                                monitorOptional.get(),
                                PageRequest.of(0, safeLimit)
                        );

        return Optional.of(incidents);
    }

    @Transactional
    public Optional<Monitor> updateMonitor(UUID id, UpdateMonitorRequest request) {

        Optional<Monitor> monitorOptional =
                monitorRepository.findById(id);

        if (monitorOptional.isEmpty()) {
            return Optional.empty();
        }

        Monitor monitor = monitorOptional.get();

        monitor.updateConfiguration(
                request.name(),
                request.url(),
                request.checkIntervalSeconds(),
                request.timeoutSeconds()
        );

        if (request.checkIntervalSeconds() != null) {
            monitor.scheduleNextCheck(Instant.now());
        }

        return Optional.of(monitor);
    }

    

}