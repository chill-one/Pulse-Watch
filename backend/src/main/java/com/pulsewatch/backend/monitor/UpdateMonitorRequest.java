package com.pulsewatch.backend.monitor;

public record UpdateMonitorRequest(
        String name,
        String url,
        Integer checkIntervalSeconds,
        Integer timeoutSeconds
) {
}