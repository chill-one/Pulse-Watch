package com.pulsewatch.backend.monitor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
}