package com.pulsewatch.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pulsewatch.common.domain.Monitor;



//Monitor -> entity this repository manages
//UUID -> type of Mointor's primary key
//provides us with save(), findByID(), findAll(), delete(), existsById()
public interface MonitorRepository
        extends JpaRepository<Monitor, UUID> {
}