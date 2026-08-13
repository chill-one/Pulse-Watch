package com.pulsewatch.common.messaging;

public final class RabbitMqNames {

    public static final String CHECK_EXCHANGE = "pulsewatch.checks";
    public static final String CHECK_QUEUE = "pulsewatch.check.tasks";
    public static final String CHECK_ROUTING_KEY = "monitor.check";

    private RabbitMqNames() {
    }
}