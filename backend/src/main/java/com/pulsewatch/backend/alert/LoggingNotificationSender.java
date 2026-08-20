package com.pulsewatch.backend.alert;

import org.springframework.stereotype.Component;

import com.pulsewatch.common.domain.Alert;

@Component
public class LoggingNotificationSender
        implements NotificationSender {

    @Override
    public void send(Alert alert) {

        System.out.println(
                "Sending notification:"
                + " alertId=" + alert.getId()
                + " type=" + alert.getType()
        );
    }
}