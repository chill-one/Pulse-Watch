package com.pulsewatch.worker.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

import com.pulsewatch.common.messaging.RabbitMqNames;

class RabbitMqConfigTest {

    private final RabbitMqConfig config = new RabbitMqConfig();

    @Test
    void workerDeclaresCompatibleMessagingTopologyAndJsonConverter() {
        DirectExchange exchange = config.checkExchange();
        Queue queue = config.checkQueue();
        Binding binding = config.checkBinding(queue, exchange);

        assertThat(exchange.getName()).isEqualTo(RabbitMqNames.CHECK_EXCHANGE);
        assertThat(exchange.isDurable()).isTrue();
        assertThat(queue.getName()).isEqualTo(RabbitMqNames.CHECK_QUEUE);
        assertThat(queue.isDurable()).isTrue();
        assertThat(binding.getExchange()).isEqualTo(RabbitMqNames.CHECK_EXCHANGE);
        assertThat(binding.getDestination()).isEqualTo(RabbitMqNames.CHECK_QUEUE);
        assertThat(binding.getRoutingKey()).isEqualTo(RabbitMqNames.CHECK_ROUTING_KEY);
        assertThat(config.messageConverter()).isInstanceOf(JacksonJsonMessageConverter.class);
    }
}
