package com.pulsewatch.backend.monitor;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.pulsewatch.common.domain.Monitor;
import com.pulsewatch.common.domain.MonitorStatus;
import com.pulsewatch.persistence.repository.MonitorRepository;



//@Service -> This class contains application/business logic and should be managed by spring
@Service
public class MonitorService {

    private final MonitorRepository monitorRepository;

    public MonitorService(MonitorRepository monitorRepository) {
        this.monitorRepository = monitorRepository;
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
}