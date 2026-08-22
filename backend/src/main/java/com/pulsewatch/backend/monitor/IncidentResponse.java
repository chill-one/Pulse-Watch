package com.pulsewatch.backend.monitor;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.pulsewatch.common.domain.Incident;

public record IncidentResponse(
        UUID id,
        Instant startedAt,
        Instant endedAt,
        Long durationSeconds
) {

    public static IncidentResponse from(Incident incident) {

        Long durationSeconds = null;

        if (incident.getEndedAt() != null) {
            durationSeconds = Duration.between(
                    incident.getStartedAt(),
                    incident.getEndedAt()
            ).getSeconds();
        }

        return new IncidentResponse(
                incident.getId(),
                incident.getStartedAt(),
                incident.getEndedAt(),
                durationSeconds
        );
    }
}