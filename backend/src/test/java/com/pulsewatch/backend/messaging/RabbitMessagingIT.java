package com.pulsewatch.backend.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import com.pulsewatch.backend.config.RabbitMqConfig;
import com.pulsewatch.common.messaging.CheckTask;
import com.pulsewatch.common.messaging.RabbitMqNames;

@SpringBootTest(classes = RabbitMessagingIT.TestApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers
class RabbitMessagingIT {

    @Container
    @ServiceConnection
    static final RabbitMQContainer RABBITMQ =
            new RabbitMQContainer("rabbitmq:4-management");

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private DirectExchange exchange;

    @Autowired
    private Queue queue;

    @Autowired
    private Binding binding;

    @Autowired
    private MessageConverter messageConverter;

    @BeforeEach
    void declareTopologyAndConverter() {
        rabbitTemplate.setMessageConverter(messageConverter);
        amqpAdmin.declareExchange(exchange);
        amqpAdmin.declareQueue(queue);
        amqpAdmin.declareBinding(binding);
    }

    @Test
    void publishesAndDeserializesCheckTaskThroughConfiguredTopology() {
        UUID taskId = UUID.randomUUID();
        UUID monitorId = UUID.randomUUID();
        Instant scheduledAt = Instant.parse("2026-01-01T00:00:00Z");
        CheckTask sent = new CheckTask(taskId, monitorId, scheduledAt);

        rabbitTemplate.convertAndSend(
                RabbitMqNames.CHECK_EXCHANGE,
                RabbitMqNames.CHECK_ROUTING_KEY,
                sent);

        Object received = rabbitTemplate.receiveAndConvert(
                RabbitMqNames.CHECK_QUEUE,
                5_000);

        assertThat(exchange.getName()).isEqualTo(RabbitMqNames.CHECK_EXCHANGE);
        assertThat(exchange.isDurable()).isTrue();
        assertThat(exchange.isAutoDelete()).isFalse();
        assertThat(queue.getName()).isEqualTo(RabbitMqNames.CHECK_QUEUE);
        assertThat(queue.isDurable()).isTrue();
        assertThat(binding.getRoutingKey()).isEqualTo(RabbitMqNames.CHECK_ROUTING_KEY);
        assertThat(received).isInstanceOf(CheckTask.class);
        assertThat((CheckTask) received).isEqualTo(sent);
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration(excludeName = {
        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
        "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"
    })
    @Import(RabbitMqConfig.class)
    static class TestApplication {
    }
}
