package com.pulsewatch.common.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class IncidentTest {

    @Test
    void startsOpenAndCanBeResolved() {
        Monitor monitor = new Monitor(
                "example",
                "https://example.test",
                60,
                10,
                Instant.now(),
                MonitorStatus.DOWN,
                3,
                Instant.now()
        );
        Instant startedAt = Instant.parse("2026-01-01T00:00:00Z");
        Incident incident = new Incident(monitor, startedAt);

        assertThat(incident.getMonitor()).isSameAs(monitor);
        assertThat(incident.getStartedAt()).isEqualTo(startedAt);
        assertThat(incident.getEndedAt()).isNull();

        Instant endedAt = Instant.parse("2026-01-01T00:05:00Z");
        incident.resolve(endedAt);

        assertThat(incident.getEndedAt()).isEqualTo(endedAt);
    }
}
