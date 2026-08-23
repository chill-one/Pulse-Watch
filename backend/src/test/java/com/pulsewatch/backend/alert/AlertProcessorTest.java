package com.pulsewatch.backend.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pulsewatch.common.domain.Alert;
import com.pulsewatch.common.domain.AlertType;
import com.pulsewatch.common.domain.Incident;
import com.pulsewatch.common.domain.Monitor;
import com.pulsewatch.common.domain.MonitorStatus;
import com.pulsewatch.persistence.repository.AlertRepository;

@ExtendWith(MockitoExtension.class)
class AlertProcessorTest {

    @Mock
    private AlertRepository alertRepository;

    @Test
    void preparesOutageNotificationWithMonitorAndIncidentDetails() {
        UUID alertId = UUID.randomUUID();
        Instant startedAt = Instant.parse("2026-01-01T00:00:00Z");
        Monitor monitor = monitor("API");
        Incident incident = new Incident(monitor, startedAt);
        Alert alert = alert(incident, AlertType.OUTAGE, alertId);
        when(alertRepository.findById(alertId)).thenReturn(Optional.of(alert));

        NotificationMessage message = processor().prepare(alertId);

        assertThat(message.alertId()).isEqualTo(alertId);
        assertThat(message.subject()).isEqualTo("[PulseWatch] DOWN : API");
        assertThat(message.body())
                .contains("Monitor: API")
                .contains("URL: https://api.example")
                .contains("Status: DOWN")
                .contains(startedAt.toString());
    }

    @Test
    void preparesRecoveryNotificationWithRecoveryTimestamp() {
        UUID alertId = UUID.randomUUID();
        Instant startedAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant endedAt = startedAt.plusSeconds(90);
        Monitor monitor = monitor("API");
        Incident incident = new Incident(monitor, startedAt);
        incident.resolve(endedAt);
        Alert alert = alert(incident, AlertType.RECOVERY, alertId);
        when(alertRepository.findById(alertId)).thenReturn(Optional.of(alert));

        NotificationMessage message = processor().prepare(alertId);

        assertThat(message.subject()).isEqualTo("[PulseWatch] RECOVERED API");
        assertThat(message.body())
                .contains("Status: UP")
                .contains(startedAt.toString())
                .contains(endedAt.toString());
    }

    @Test
    void missingAlertIsRejectedForPreparationAndStateUpdates() {
        UUID alertId = UUID.randomUUID();
        when(alertRepository.findById(alertId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> processor().prepare(alertId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Alert not found: " + alertId);
        assertThatThrownBy(() -> processor().markSent(alertId, Instant.now()))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> processor().markFailed(alertId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void marksExistingAlertSentOrFailed() {
        UUID alertId = UUID.randomUUID();
        Alert alert = alert(new Incident(monitor("API"), Instant.now()), AlertType.OUTAGE, alertId);
        when(alertRepository.findById(alertId)).thenReturn(Optional.of(alert));
        Instant sentAt = Instant.parse("2026-01-01T00:00:05Z");

        processor().markSent(alertId, sentAt);
        assertThat(alert.getDeliveryStatus()).isEqualTo(com.pulsewatch.common.domain.DeliveryStatus.SENT);
        assertThat(alert.getSentAt()).isEqualTo(sentAt);

        processor().markFailed(alertId);
        assertThat(alert.getDeliveryStatus()).isEqualTo(com.pulsewatch.common.domain.DeliveryStatus.FAILED);
        verify(alertRepository, org.mockito.Mockito.times(2)).findById(alertId);
    }

    private AlertProcessor processor() {
        return new AlertProcessor(alertRepository);
    }

    private static Monitor monitor(String name) {
        return new Monitor(
                name,
                "https://api.example",
                60,
                10,
                Instant.now(),
                MonitorStatus.DOWN,
                3,
                Instant.now()
        );
    }

    private static Alert alert(Incident incident, AlertType type, UUID id) {
        Alert alert = new Alert(incident, type, Instant.now());
        org.springframework.test.util.ReflectionTestUtils.setField(alert, "id", id);
        return alert;
    }
}
