package com.pulsewatch.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pulsewatch.common.domain.CheckResult;

public interface CheckResultRepository
        extends JpaRepository<CheckResult, UUID> {

    boolean existsByTaskId(UUID taskId);
}