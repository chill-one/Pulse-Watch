package com.pulsewatch.backend.monitor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateMonitorRequest(

        //@NotBlank -> for String Values and Rejects null, "", and whitespace-only string

        @NotBlank(message = "Name is requried")
        String name,

        @NotBlank(message = "Url is required")
        String url,

        //@Postive -> numeric value must be > 0
        @Positive(message = "CheckIntervalSeconds must be greater than 0")
        int checkIntervalSeconds,

        @Positive(message = "TimeoutSeconds must be greater than 0")
        int timeoutSeconds
) {
}