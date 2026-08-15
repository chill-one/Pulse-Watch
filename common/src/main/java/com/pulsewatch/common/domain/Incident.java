package com.pulsewatch.common.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monitor_id", nullable = false)
    private Monitor monitor;

    @Column(nullable = false)
    private Instant startedAt;


    //null means the outage is still happening
    private Instant endedAt;

    protected Incident() {}

    public Incident(
        Monitor monitor,
        Instant startedAt) {

        this.monitor = monitor;
        this.startedAt = startedAt;
        this.endedAt = null;
    }

    //reads naturally compared to setEndAT()
    public void resolve(Instant resolvedAt) {
        this.endedAt = resolvedAt;
    }


    public UUID getId() {
        return id;
    }

    public Monitor getMonitor() {
        return monitor;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }
}