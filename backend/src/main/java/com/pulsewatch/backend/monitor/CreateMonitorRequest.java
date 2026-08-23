package com.pulsewatch.backend.monitor;

import java.net.URI;
import java.net.URISyntaxException;

import jakarta.validation.constraints.AssertTrue;
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



    @AssertTrue(message = "url must be a valid HTTP or HTTPS URL")
    public boolean isUrlValid() {

        //Without this check both @Not blank and AssertTrue can generate two validation error
        //Since @Not blank already owns this rule we return true
        if (url == null || url.isBlank()) {
            return true;
        }

        try {
            //url given: https://google.com
            URI uri = new URI(url);

            //https or http
            String scheme = uri.getScheme();

            //host -> google.com
            boolean httpScheme = "http".equalsIgnoreCase(scheme);
            boolean httpsScheme = "https".equalsIgnoreCase(scheme);
            return uri.getHost() != null && (httpScheme || httpsScheme);

        } catch (URISyntaxException e) {
            return false;
        }
    }
}
