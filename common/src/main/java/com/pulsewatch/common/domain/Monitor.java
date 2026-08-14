package com.pulsewatch.common.domain;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;



//@Entity -> This java class represents presistent data that should be mapped to database table
@Entity
public class Monitor {

    //@ID -> This field is the primary identifier for each Monitor
    //@GeneratedValue(strategy = GenerationType.UUID) -> JPA should generate the UUID for us when new Monitor is persisited.
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
}