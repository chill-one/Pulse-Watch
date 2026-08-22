package com.pulsewatch.persistence.repository;

import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pulsewatch.common.domain.Alert;
import com.pulsewatch.common.domain.DeliveryStatus;
import com.pulsewatch.common.domain.Monitor;

public interface AlertRepository
        extends JpaRepository<Alert, UUID> {
        

        /**
         * find 
         * top 50 -> process at most 50 at a time
         * ByDeliveryStatus -> Where delivery_status = ?
         * OrderdByCreatedAtAsc -> oldest alerts first
         */
        List<Alert> findTop50ByDeliveryStatusOrderByCreatedAtAsc(
                DeliveryStatus deliveryStatus
        );


        void deleteByIncidentMonitor(Monitor monitor);
}