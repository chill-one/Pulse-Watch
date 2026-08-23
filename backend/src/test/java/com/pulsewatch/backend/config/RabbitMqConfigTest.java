package com.pulsewatch.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

import com.pulsewatch.common.messaging.RabbitMqNames;

class RabbitMqConfigTest {

    private final RabbitMqConfig config = new RabbitMqConfig();

    @Test
    void declaresDurableCheckExchangeAndQueue() {
        DirectExchange exchange = config.checkExchange();
        Queue queue = config.checkQueue();

        assertThat(exchange.getName()).isEqualTo(RabbitMqNames.CHECK_EXCHANGE);
        assertThat(exchange.isDurable()).isTrue();
        assertThat(exchange.isAutoDelete()).isFalse();
        assertThat(queue.getName()).isEqualTo(RabbitMqNames.CHECK_QUEUE);
        assertThat(queue.isDurable()).isTrue();
    }

    @Test
    void bindsQueueToExchangeWithCheckRoutingKeyAndUsesJson() {
        Queue queue = config.checkQueue();
        DirectExchange exchange = config.checkExchange();
        Binding binding = config.checkBinding(queue, exchange);

        assertThat(binding.getDestination()).isEqualTo(RabbitMqNames.CHECK_QUEUE);
        assertThat(binding.getExchange()).isEqualTo(RabbitMqNames.CHECK_EXCHANGE);
        assertThat(binding.getRoutingKey()).isEqualTo(RabbitMqNames.CHECK_ROUTING_KEY);
        MessageConverter converter = config.messageConverter();
        assertThat(converter).isInstanceOf(JacksonJsonMessageConverter.class);
    }
}
