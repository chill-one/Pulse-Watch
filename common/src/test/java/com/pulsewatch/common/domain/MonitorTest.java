package com.pulsewatch.common.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class MonitorTest {

    @Test
    void schedulesNextCheckFromSuppliedInstant() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Monitor monitor = monitor(createdAt.plusSeconds(60));

        Instant from = Instant.parse("2026-01-01T01:00:00Z");
        monitor.scheduleNextCheck(from);

        assertThat(monitor.getNextCheckAt()).isEqualTo(from.plusSeconds(60));
    }

    @Test
    void recordsSuccessAndResetsFailureState() {
        Monitor monitor = monitor(Instant.now());
        monitor.recordFailure(3);
        monitor.recordFailure(3);

        monitor.recordSucess();

        assertThat(monitor.getStatus()).isEqualTo(MonitorStatus.UP);
        assertThat(monitor.getConsecutiveFailureCount()).isZero();
    }

    @Test
    void recordsFailuresAsDegradedUntilThresholdThenDown() {
        Monitor monitor = monitor(Instant.now());

        monitor.recordFailure(3);
        assertThat(monitor.getStatus()).isEqualTo(MonitorStatus.DEGRADED);
        assertThat(monitor.getConsecutiveFailureCount()).isOne();

        monitor.recordFailure(3);
        assertThat(monitor.getStatus()).isEqualTo(MonitorStatus.DEGRADED);

        monitor.recordFailure(3);
        assertThat(monitor.getStatus()).isEqualTo(MonitorStatus.DOWN);
        assertThat(monitor.getConsecutiveFailureCount()).isEqualTo(3);
    }

    @Test
    void rejectsNonPositiveFailureThreshold() {
        Monitor monitor = monitor(Instant.now());

        assertThatThrownBy(() -> monitor.recordFailure(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("failureThreshold must be greater than 0");
    }

    @Test
    void updatesOnlyNonNullConfigurationValues() {
        Monitor monitor = new Monitor(
                "original",
                "https://original.example",
                60,
                10,
                Instant.parse("2026-01-01T00:01:00Z"),
                MonitorStatus.UP,
                2,
                Instant.parse("2026-01-01T00:00:00Z")
        );

        monitor.updateConfiguration("renamed", null, 120, null);

        assertThat(monitor.getName()).isEqualTo("renamed");
        assertThat(monitor.getUrl()).isEqualTo("https://original.example");
        assertThat(monitor.getCheckIntervalSeconds()).isEqualTo(120);
        assertThat(monitor.getTimeoutSeconds()).isEqualTo(10);
        assertThat(monitor.getStatus()).isEqualTo(MonitorStatus.UP);
        assertThat(monitor.getConsecutiveFailureCount()).isEqualTo(2);
    }

    private static Monitor monitor(Instant nextCheckAt) {
        return new Monitor(
                "example",
                "https://example.test",
                60,
                10,
                nextCheckAt,
                MonitorStatus.PENDING,
                0,
                Instant.parse("2026-01-01T00:00:00Z")
        );
    }
}
