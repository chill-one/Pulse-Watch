package com.pulsewatch.backend.alert;

public interface NotificationSender {

    void send(NotificationMessage message);
}