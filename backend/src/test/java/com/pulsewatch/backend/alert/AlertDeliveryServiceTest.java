package com.pulsewatch.backend.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pulsewatch.common.domain.Alert;
import com.pulsewatch.common.domain.AlertType;
import com.pulsewatch.common.domain.Incident;
import com.pulsewatch.common.domain.Monitor;
import com.pulsewatch.common.domain.MonitorStatus;
import com.pulsewatch.common.domain.DeliveryStatus;
import com.pulsewatch.persistence.repository.AlertRepository;

@ExtendWith(MockitoExtension.class)
class AlertDeliveryServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private NotificationSender notificationSender;

    @Mock
    private AlertProcessor alertProcessor;

    @Test
    void doesNothingWhenThereAreNoPendingAlerts() {
        when(alertRepository.findTop50ByDeliveryStatusOrderByCreatedAtAsc(DeliveryStatus.PENDING))
                .thenReturn(List.of());

        service().pollPendingAlerts();

        verify(notificationSender, never()).send(any());
        verify(alertProcessor, never()).prepare(any());
    }

    @Test
    void sendsPendingAlertAndMarksItSentWithTimestamp() {
        UUID id = UUID.randomUUID();
        Alert alert = alert(id);
        NotificationMessage message = new NotificationMessage(id, "subject", "body");
        when(alertRepository.findTop50ByDeliveryStatusOrderByCreatedAtAsc(DeliveryStatus.PENDING))
                .thenReturn(List.of(alert));
        when(alertProcessor.prepare(id)).thenReturn(message);

        Instant before = Instant.now();
        service().pollPendingAlerts();
        Instant after = Instant.now();

        verify(notificationSender).send(message);
        ArgumentCaptor<Instant> sentAt = ArgumentCaptor.forClass(Instant.class);
        verify(alertProcessor).markSent(org.mockito.ArgumentMatchers.eq(id), sentAt.capture());
        assertThat(sentAt.getValue()).isBetween(before, after);
        verify(alertProcessor, never()).markFailed(any());
    }

    @Test
    void marksFailedAlertAndContinuesWithLaterAlerts() {
        UUID failedId = UUID.randomUUID();
        UUID successfulId = UUID.randomUUID();
        Alert failed = alert(failedId);
        Alert successful = alert(successfulId);
        NotificationMessage failedMessage = new NotificationMessage(failedId, "failed", "body");
        NotificationMessage successfulMessage = new NotificationMessage(successfulId, "success", "body");
        when(alertRepository.findTop50ByDeliveryStatusOrderByCreatedAtAsc(DeliveryStatus.PENDING))
                .thenReturn(List.of(failed, successful));
        when(alertProcessor.prepare(failedId)).thenReturn(failedMessage);
        when(alertProcessor.prepare(successfulId)).thenReturn(successfulMessage);
        org.mockito.Mockito.doThrow(new IllegalStateException("SES unavailable"))
                .when(notificationSender).send(failedMessage);

        service().pollPendingAlerts();

        verify(alertProcessor).markFailed(failedId);
        verify(notificationSender).send(successfulMessage);
        verify(alertProcessor).markSent(org.mockito.ArgumentMatchers.eq(successfulId), any(Instant.class));
    }

    private AlertDeliveryService service() {
        return new AlertDeliveryService(alertRepository, notificationSender, alertProcessor);
    }

    private static Alert alert(UUID id) {
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
        Alert alert = new Alert(new Incident(monitor, Instant.now()), AlertType.OUTAGE, Instant.now());
        org.springframework.test.util.ReflectionTestUtils.setField(alert, "id", id);
        return alert;
    }
}
