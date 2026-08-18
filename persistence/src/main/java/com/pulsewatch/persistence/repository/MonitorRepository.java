package com.pulsewatch.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pulsewatch.common.domain.Monitor;



//Monitor -> entity this repository manages
//UUID -> type of Mointor's primary key
//provides us with save(), findByID(), findAll(), delete(), existsById()
public interface MonitorRepository extends JpaRepository<Monitor, UUID> {


    /**
     * Top 200 -> don't grab every due mointor at once
     * ByNextCheckAtLessThanEqual -> nextCheckAt <= now
     * OrderByNextCheckAtAsc -> oldest overdue mointors first
     * @param now Current time
     * @return All the mointor that are due
     */
    List<Monitor>
        findTop200ByNextCheckAtLessThanEqualOrderByNextCheckAtAsc(
            Instant now
        );


        
}