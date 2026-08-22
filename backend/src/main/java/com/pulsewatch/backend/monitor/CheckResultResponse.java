package com.pulsewatch.backend.monitor;

import java.time.Instant;
import java.util.UUID;

import com.pulsewatch.common.domain.CheckError;
import com.pulsewatch.common.domain.CheckResult;

public record CheckResultResponse(
        UUID id,
        Instant checkedAt,
        Integer statusCode,
        long latencyMs,
        CheckError error
) {

    public static CheckResultResponse from(CheckResult result) {
        return new CheckResultResponse(
                result.getId(),
                result.getCheckedAt(),
                result.getStatusCode(),
                result.getLatencyMs(),
                result.getError()
        );
    }
}