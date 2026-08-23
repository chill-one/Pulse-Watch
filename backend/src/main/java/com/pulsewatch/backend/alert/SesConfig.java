package com.pulsewatch.backend.alert;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;

@Configuration
@Profile("ses-email")
public class SesConfig {

    @Bean
    public SesV2Client sesV2Client(
            @Value("${pulsewatch.email.aws-region}") String region) {

        return SesV2Client.builder()
                .region(Region.of(region))
                .build();
    }
}