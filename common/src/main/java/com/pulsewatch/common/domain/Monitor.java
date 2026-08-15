package com.pulsewatch.common.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;




//@Entity -> This java class represents presistent data that should be mapped to database table
@Entity
public class Monitor {
    //@ID -> This field is the primary identifier for each Monitor
    //@GeneratedValue(strategy = GenerationType.UUID) -> JPA should generate the UUID for us when new Monitor is persisited.
    //@Column(nullable = False) -> the row of this column cannot be null
    //@Enumerated(EnumType.STRING) -> Tells JPA to store the enum using its name 
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;


    @Column(nullable = false)
    private String name;


    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private int checkIntervalSeconds;

    @Column(nullable = false)
    private int timeoutSeconds;

    @Column(nullable = false)
    private Instant nextCheckAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MonitorStatus status;

    @Column(nullable = false)
    private int consecutiveFailureCount;

    protected Monitor(){}

    public Monitor(
        String name,
        String url,
        int checkIntervalSeconds,
        int timeoutSeconds,
        Instant nextCheckAt,
        MonitorStatus status,
        int consecutiveFailureCount,
        Instant createdAt) {

        this.name = name;
        this.url = url;
        this.checkIntervalSeconds = checkIntervalSeconds;
        this.timeoutSeconds = timeoutSeconds;
        this.nextCheckAt = nextCheckAt;
        this.status = status;
        this.consecutiveFailureCount = consecutiveFailureCount;
        this.createdAt = createdAt;

    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public int getCheckIntervalSeconds() {
        return checkIntervalSeconds;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public Instant getNextCheckAt() {
        return nextCheckAt;
    }

    public MonitorStatus getStatus() {
        return status;
    }

    public int getConsecutiveFailureCount() {
        return consecutiveFailureCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
