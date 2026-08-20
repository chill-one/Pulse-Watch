package com.pulsewatch.backend.alert;

import com.pulsewatch.common.domain.Alert;

public interface NotificationSender {

    void send(Alert alert);
}