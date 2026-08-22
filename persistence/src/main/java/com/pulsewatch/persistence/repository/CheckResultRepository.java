package com.pulsewatch.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.pulsewatch.common.domain.CheckResult;
import com.pulsewatch.common.domain.Monitor;


public interface CheckResultRepository
        extends JpaRepository<CheckResult, UUID> {

    boolean existsByTaskId(UUID taskId);
    

    //Find the check results where monitor is ? orderd by checkedAt DESC apply Pageable limit
    List<CheckResult> findByMonitorOrderByCheckedAtDesc(
        Monitor monitor,
        Pageable pageable
    );
}