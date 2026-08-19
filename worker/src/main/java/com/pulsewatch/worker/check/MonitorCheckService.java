package com.pulsewatch.worker.check;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.pulsewatch.persistence.repository.MonitorRepository;

@Service
public class MonitorCheckService {

    private final MonitorRepository monitorRepository;
    private final RestClient.Builder restClientBuilder;

    public MonitorCheckService(
            MonitorRepository monitorRepository,
            RestClient.Builder restClientBuilder) {

        this.monitorRepository = monitorRepository;
        this.restClientBuilder = restClientBuilder;
    }
}