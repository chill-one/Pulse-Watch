package com.pulsewatch.backend.alert;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.pulsewatch.common.domain.Alert;
import com.pulsewatch.common.domain.DeliveryStatus;
import com.pulsewatch.persistence.repository.AlertRepository;

@Service
public class AlertDeliveryService {

    private final AlertRepository alertRepository;
    private final AlertProcessor alertProcessor;

    public AlertDeliveryService(
            AlertRepository alertRepository,
            AlertProcessor alertProcessor) {

        this.alertRepository = alertRepository;
        this.alertProcessor = alertProcessor;
    }


    @Scheduled(fixedDelayString = "${pulsewatch.alerts.poll-delay-ms:5000}")
    public void pollPendingAlerts() {

        List<Alert> pendingAlerts = 
                alertRepository
                        .findTop50ByDeliveryStatusOrderByCreatedAtAsc(DeliveryStatus.PENDING);


        for (Alert alert : pendingAlerts) {
            alertProcessor.deliver(alert);
        }
    }

}