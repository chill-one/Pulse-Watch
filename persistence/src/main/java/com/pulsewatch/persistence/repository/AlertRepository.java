package com.pulsewatch.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pulsewatch.common.domain.Alert;

public interface AlertRepository
        extends JpaRepository<Alert, UUID> {
}