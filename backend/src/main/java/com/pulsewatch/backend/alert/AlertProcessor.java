package com.pulsewatch.backend.alert;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulsewatch.common.domain.Alert;
import com.pulsewatch.common.domain.Incident;
import com.pulsewatch.common.domain.Monitor;
import com.pulsewatch.persistence.repository.AlertRepository;

@Service
public class AlertProcessor {

    private final AlertRepository alertRepository;
    private final NotificationSender notificationSender;

    public AlertProcessor(
            AlertRepository alertRepository,
            NotificationSender notificationSender) {

        this.alertRepository = alertRepository;
        this.notificationSender = notificationSender;
    }

    @Transactional(readOnly = true)
    public NotificationMessage prepare(UUID alertId) {

        Alert alert = alertRepository
                .findById(alertId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Alert not found: " + alertId
                        )
                );

        Incident incident = alert.getIncident();
        Monitor monitor = incident.getMonitor();

        String subject = buildSubject(alert, monitor);
        String body = buildBody(alert, monitor, incident);

        return new NotificationMessage(
                alert.getId(),
                subject,
                body
        );
    }

    @Transactional
    public void markSent(UUID alertId, Instant sentAt) {

        Alert alert = alertRepository
                .findById(alertId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Alert not found: " + alertId
                        )
                );

        alert.markSent(sentAt);
    }

    @Transactional
    public void markFailed(UUID alertId) {

        Alert alert = alertRepository
                .findById(alertId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Alert not found: " + alertId
                        )
                );

        alert.markFailed();
    }

}