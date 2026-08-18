package com.pulsewatch.backend.scheduler;

import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.pulsewatch.common.domain.Monitor;
import com.pulsewatch.persistence.repository.MonitorRepository;


@Component
public class MonitorScheduler{
    
    private final MonitorRepository monitorRepository;

    public MonitorScheduler(MonitorRepository monitorRepository){
        this.monitorRepository = monitorRepository;
    }

    //A fixed delay schedules the next execution relative to completion of the previous execution. 
    // Spring's @Scheduled supports fixed-delay, fixed-rate, and cron-style scheduling
    @Scheduled(
        fixedDelayString = "${pulsewatch.scheduler.poll-delay-ms:5000}"
    )
    public void pollDueMonitors() {
        
        Instant now = Instant.now();

        List<Monitor> dueMonitors = 
            monitorRepository
            .findTop200ByNextCheckAtLessThanEqualOrderByNextCheckAtAsc(
                now
            );

        System.out.println("Found " + dueMonitors.size() + " due monitors");
    }
}