package com.pulsewatch.worker.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.pulsewatch.common.messaging.RabbitMqNames;

// @RabbitListner -> Spring should create RabbitMQ listner for this method
// @Component -> Spring should discover, instantiate, and manage this class
@Component
public class CheckTaskConsumer {

    /**
     * Spring, keep listening to the RabbitMQ queue named pulsewatch.check.tasks.
     * Whenever RabbitMQ delivers a message from that queue, call this method.
     * @param message
     */
    @RabbitListener(queues = RabbitMqNames.CHECK_QUEUE)
    public void receive(String message){
        // For today's test only:
        // print the message that RabbitMQ delivered.
         System.out.println("Received check task: " + message);
    }
    
}
