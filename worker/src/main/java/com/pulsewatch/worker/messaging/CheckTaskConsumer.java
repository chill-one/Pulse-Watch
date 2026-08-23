package com.pulsewatch.worker.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.pulsewatch.common.messaging.CheckTask;
import com.pulsewatch.common.messaging.RabbitMqNames;
import com.pulsewatch.worker.check.MonitorCheckService;


// @RabbitListner -> Spring should create RabbitMQ listner for this method
// @Component -> Spring should discover, instantiate, and manage this class
@Component
public class CheckTaskConsumer {

    private final MonitorCheckService monitorCheckService;

    public CheckTaskConsumer(MonitorCheckService monitorCheckService) {
        this.monitorCheckService = monitorCheckService;
    }

    /**
     * Spring, keep listening to the RabbitMQ queue named pulsewatch.check.tasks.
     * Whenever RabbitMQ delivers a message from that queue, call this method.
     * @param message
     */
    @RabbitListener(queues = RabbitMqNames.CHECK_QUEUE)
    public void receive(CheckTask task) {
        monitorCheckService.process(task);
    }
    
}
