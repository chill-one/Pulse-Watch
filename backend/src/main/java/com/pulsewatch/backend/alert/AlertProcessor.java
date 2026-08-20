package com.pulsewatch.backend.alert;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulsewatch.common.domain.Alert;
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

    @Transactional
    public void deliver(Alert alert) {

        try {

            notificationSender.send(alert);

            alert.markSent(Instant.now());

        } catch (RuntimeException e) {

            alert.markFailed();
        }

        alertRepository.save(alert);
    }
}