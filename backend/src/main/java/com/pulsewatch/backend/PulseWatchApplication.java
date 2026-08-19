package com.pulsewatch.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;




//This tells Spring this is the starting point
@SpringBootApplication
//Activates Spring's Infrastrucutre for methods annoted with @Scheduled which is in MointorRepository.java
@EnableScheduling
//Tells Spring that my @Entity classes are over here.
@EntityScan(basePackages = "com.pulsewatch.common.domain")
//Tells Spring Data repository interface are ove here.
@EnableJpaRepositories(basePackages = "com.pulsewatch.persistence.repository")
public class PulseWatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(PulseWatchApplication.class, args);
    }
}