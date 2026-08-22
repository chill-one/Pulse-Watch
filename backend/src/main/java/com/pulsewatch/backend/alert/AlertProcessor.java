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

    public AlertProcessor(
            AlertRepository alertRepository) {

        this.alertRepository = alertRepository;
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

    private String buildSubject(Alert alert, Monitor monitor) {

        return switch (alert.getType()) {
                case OUTAGE ->
                        "[PulseWatch] DOWN : " + monitor.getName();
                case RECOVERY ->
                        "[PulseWatch] RECOVERED " + monitor.getName();
        };
    }


    private String buildBody(Alert alert, Monitor monitor, Incident incident) {

    return switch (alert.getType()) {

        case OUTAGE -> """
                PulseWatch detected an outage.

                Monitor: %s
                URL: %s
                Status: DOWN
                Outage started: %s
                """.formatted(
                        monitor.getName(),
                        monitor.getUrl(),
                        incident.getStartedAt()
                );

        case RECOVERY -> """
                PulseWatch detected a recovery.

                Monitor: %s
                URL: %s
                Status: UP
                Outage started: %s
                Recovered at: %s
                """.formatted(
                        monitor.getName(),
                        monitor.getUrl(),
                        incident.getStartedAt(),
                        incident.getEndedAt()
                );
    };
}

}