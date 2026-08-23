package com.pulsewatch.backend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.pulsewatch.common.messaging.RabbitMqNames;

//@Configuration -> This class contains objects that Spring should create/configure when the application starts.
//@Bean -> Spring call this method and manage the returned object for me


@Configuration
public class RabbitMqConfig {

    
    /**
     * 
     * A direct exchange routes messages by matching a routing key.
     * @return DirectExchange object
     * 
     *  Message routing key == binding routing key -> send to message queue.
     */
    @Bean
    public DirectExchange checkExchange() {
        // Set the direct exchange route
        return new DirectExchange(
                RabbitMqNames.CHECK_EXCHANGE,
                true,
                false
        );
    }

    /**
     * The queue is where message actually wait for worker.
     * @return Queue Object
     */
    @Bean
    public Queue checkQueue() {
        // Create a durable queue so the queue definition can survive
        // a RabbitMQ broker restart.
        return new Queue(
                RabbitMqNames.CHECK_QUEUE,
                true
        );
    }

    /**
     * This is the rule that is able to connect a messge sent to exchange with routing key montior.check and direct it to the queue.
     * @param checkQueue    The checkQueue object
     * @param checkExchange The directExchange object
     * @return
     */
    @Bean
    public Binding checkBinding(
            Queue checkQueue,
            DirectExchange checkExchange) {
        
        //Bind checkQueue to checkExchange with routing key
        return BindingBuilder
                .bind(checkQueue)
                .to(checkExchange)
                .with(RabbitMqNames.CHECK_ROUTING_KEY);
    }

    /**
     * We want RabbitMQ to contain JSON rather than Java serialization.
     * @return
     */
    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter("com.pulsewatch.common.messaging");
    }
}
