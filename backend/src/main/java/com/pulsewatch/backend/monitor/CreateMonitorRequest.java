package com.pulsewatch.backend.monitor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateMonitorRequest(

        @NotBlank(message = "Name is requried")
        String name,

        @NotBlank(message = "Url is required")
        String url,

        @Positive(message = "CheckIntervalSeconds must be greater than 0")
        int checkIntervalSeconds,

        @Positive(message = "TimeoutSeconds must be greater than 0")
        int timeoutSeconds
) {
}