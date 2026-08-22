package com.pulsewatch.backend.alert;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!ses-email")
public class LoggingNotificationSender
        implements NotificationSender {

    @Override
    public void send(NotificationMessage message) {

        System.out.println(
                "Sending notification:"
                + " alertId=" + message.alertId()
                + " subject=" + message.subject()
        );
    }
}