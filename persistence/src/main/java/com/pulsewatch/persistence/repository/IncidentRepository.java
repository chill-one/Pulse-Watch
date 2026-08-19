package com.pulsewatch.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pulsewatch.common.domain.Incident;
import com.pulsewatch.common.domain.Monitor;

public interface IncidentRepository
        extends JpaRepository<Incident, UUID> {
    

    /**
     * Find first Incident
     *  where monitor = ?
     *  AND endedAt IS NULL
     *  ordered by startedAt newest first
     * @param monitor
     * @return
     */
    Optional<Incident>
        findFirstByMonitorAndEndedAtIsNullOrderByStartedAtDesc(
            Monitor monitor
        );
}