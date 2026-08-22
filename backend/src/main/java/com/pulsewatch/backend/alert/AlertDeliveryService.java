package com.pulsewatch.backend.alert;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.pulsewatch.common.domain.Alert;
import com.pulsewatch.common.domain.DeliveryStatus;
import com.pulsewatch.persistence.repository.AlertRepository;

@Service
public class AlertDeliveryService {

    private final AlertRepository alertRepository;
    private final AlertProcessor alertProcessor;
    private final NotificationSender notificationSender;

    public AlertDeliveryService(
            AlertRepository alertRepository,
            NotificationSender notificationSender,
            AlertProcessor alertProcessor) {

        this.alertRepository = alertRepository;
        this.alertProcessor = alertProcessor;
        this.notificationSender = notificationSender;
    }

    @Scheduled(
        fixedDelayString = "${pulsewatch.alerts.poll-delay-ms:5000}"
    )
    public void pollPendingAlerts() {

        List<Alert> pendingAlerts =
                alertRepository
                        .findTop50ByDeliveryStatusOrderByCreatedAtAsc(
                                DeliveryStatus.PENDING
                        );

        System.out.println(
                "Pending alerts found: " + pendingAlerts.size()
        );

        for (Alert alert : pendingAlerts) {

            UUID alertId = alert.getId();

            NotificationMessage message =
                    alertProcessor.prepare(alertId);

            try {

                notificationSender.send(message);

                alertProcessor.markSent(
                        alertId,
                        Instant.now()
                );

            } catch (RuntimeException e) {

                System.err.println(
                        "Failed to send alert "
                        + alertId
                        + ": "
                        + e.getMessage()
                );

                alertProcessor.markFailed(alertId);
            }
        }
    }
}