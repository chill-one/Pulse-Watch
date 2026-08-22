package com.pulsewatch.backend.alert;

import java.util.UUID;

public record NotificationMessage(
        UUID alertId,
        String subject,
        String body
) {
}