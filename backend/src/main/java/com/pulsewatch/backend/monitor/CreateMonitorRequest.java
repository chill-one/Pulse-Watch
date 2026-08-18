package com.pulsewatch.backend.monitor;

public record CreateMonitorRequest(
        String name,
        String url,
        int checkIntervalSeconds,
        int timeoutSeconds
) {
}