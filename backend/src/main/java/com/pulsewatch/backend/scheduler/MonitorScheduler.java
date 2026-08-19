package com.pulsewatch.backend.scheduler;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.pulsewatch.common.domain.Monitor;
import com.pulsewatch.common.messaging.CheckTask;
import com.pulsewatch.common.messaging.RabbitMqNames;
import com.pulsewatch.persistence.repository.MonitorRepository;


@Component
public class MonitorScheduler{
    
    private final MonitorRepository monitorRepository;
    private final RabbitTemplate rabbitTemplate;

    public MonitorScheduler(MonitorRepository monitorRepository, RabbitTemplate rabbitTemplate){
        this.monitorRepository = monitorRepository;
        this.rabbitTemplate = rabbitTemplate;
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
        for (Monitor monitor : dueMonitors){
            CheckTask task = new CheckTask(
                UUID.randomUUID(),
                monitor.getId(),
                monitor.getNextCheckAt()
            );

            rabbitTemplate.convertAndSend(
                RabbitMqNames.CHECK_EXCHANGE,
                RabbitMqNames.CHECK_ROUTING_KEY,
                task
            );
            
            monitor.scheduleNextCheck(now);

            monitorRepository.save(monitor);
        }
    }
}