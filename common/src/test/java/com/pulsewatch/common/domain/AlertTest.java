package com.pulsewatch.common.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class AlertTest {

    @Test
    void startsPendingAndCanBeMarkedSent() {
        Incident incident = incident();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Alert alert = new Alert(incident, AlertType.OUTAGE, createdAt);

        assertThat(alert.getDeliveryStatus()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(alert.getSentAt()).isNull();

        Instant sentAt = createdAt.plusSeconds(5);
        alert.markSent(sentAt);

        assertThat(alert.getDeliveryStatus()).isEqualTo(DeliveryStatus.SENT);
        assertThat(alert.getSentAt()).isEqualTo(sentAt);
    }

    @Test
    void canBeMarkedFailed() {
        Alert alert = new Alert(incident(), AlertType.RECOVERY, Instant.now());

        alert.markFailed();

        assertThat(alert.getDeliveryStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(alert.getSentAt()).isNull();
    }

    private static Incident incident() {
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
        return new Incident(monitor, Instant.parse("2026-01-01T00:00:00Z"));
    }
}
