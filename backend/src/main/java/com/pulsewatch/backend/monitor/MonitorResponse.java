package com.pulsewatch.backend.monitor;

import java.time.Instant;
import java.util.UUID;

import com.pulsewatch.common.domain.Monitor;
import com.pulsewatch.common.domain.MonitorStatus;


//What we send back to the client
public record MonitorResponse(
        UUID id,
        String name,
        String url,
        int checkIntervalSeconds,
        int timeoutSeconds,
        Instant nextCheckAt,
        MonitorStatus status,
        int consecutiveFailureCount,
        Instant createdAt
) {

    public static MonitorResponse from(Monitor monitor) {
        return new MonitorResponse(
                monitor.getId(),
                monitor.getName(),
                monitor.getUrl(),
                monitor.getCheckIntervalSeconds(),
                monitor.getTimeoutSeconds(),
                monitor.getNextCheckAt(),
                monitor.getStatus(),
                monitor.getConsecutiveFailureCount(),
                monitor.getCreatedAt()
        );
    }
}