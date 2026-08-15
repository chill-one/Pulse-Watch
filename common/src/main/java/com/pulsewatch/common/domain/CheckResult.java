package com.pulsewatch.common.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;


@Entity
public class CheckResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
    Reason beind unique = True
    scenario  -> if RabbitMQ sends Task ABC -> Worker process it
        -> checkResult saved -> taskId = ABC -> worker 'crashes' Before ACK (acknowledgment)
        -> RabbitMQ redelivers Task ABC
    */
    @Column(nullable = false, unique = true)
    private UUID taskId;



    //@ManyToOne -> Many CheckResult object may reference the same Monitor
    // optional = false means a CheckResult is not allowed to exist wihtout an Mointor
    // fetch = FetchType.LAZY -> Don't auto load the entire Mointor object every single time I fetch checkresult unless i need it

    //@JointColumn -> In the CheckResult Table, use a column named monitor_id to refernec the associated Mointor
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monitor_id", nullable = false)
    private Monitor monitor;


    // When did this individual HTTP-check attempt happen?
    @Column(nullable = false)
    private Instant checkedAt;


    //null indicates we could have an error 
    @Column
    private Integer statusCode;

    // Total duration of the HTTP attempt, whether it succeds or fails
    @Column(nullable = false)
    private long latencyMs;

    //if statusCode is null than this will show us the error type
    @Enumerated(EnumType.STRING)
    private CheckError error;

    //JPA Constructor
    protected CheckResult() {}


    public CheckResult(
        UUID taskId,
        Monitor monitor,
        Instant checkedAt,
        Integer statusCode,
        long latencyMs,
        CheckError error) {

    this.taskId = taskId;
    this.monitor = monitor;
    this.checkedAt = checkedAt;
    this.statusCode = statusCode;
    this.latencyMs = latencyMs;
    this.error = error;
    }


    public UUID getId() {
        return id;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public Monitor getMonitor() {
        return monitor;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public CheckError getError() {
        return error;
    }
}
