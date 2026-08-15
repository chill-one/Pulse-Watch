package com.pulsewatch.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.pulsewatch.common.domain")
@EnableJpaRepositories(basePackages = "com.pulsewatch.persistence.repository")
public class PulseWatchWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PulseWatchWorkerApplication.class, args);
    }
}