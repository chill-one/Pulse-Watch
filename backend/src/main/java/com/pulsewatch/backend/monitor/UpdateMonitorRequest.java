package com.pulsewatch.backend.monitor;

import java.net.URI;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateMonitorRequest(

        @Size(max = 100)
        String name,

        @Size(max = 2048)
        String url,

        @Positive
        Integer checkIntervalSeconds,

        @Positive
        Integer timeoutSeconds
) {

    @AssertTrue(message = "name must not be blank")
    public boolean isNameValid() {
        return name == null || !name.isBlank();
    }

    @AssertTrue(message = "url must be a valid HTTP or HTTPS URL")
    public boolean isUrlValid() {

        if (url == null) {
            return true;
        }

        try {
            URI uri = URI.create(url);

            return uri.getHost() != null
                    && ("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()));

        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}