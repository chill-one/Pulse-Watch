package com.pulsewatch.worker.check;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.pulsewatch.common.domain.Monitor;
import com.pulsewatch.common.messaging.CheckTask;
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

    /**
     * Gets the monitor associtated with the given task monitor Id and sends a request
     * @param task The message object for the current task
     */
    public void process(CheckTask task) {

        Monitor monitor = monitorRepository
                          .findById(task.monitorId())
                          .orElse(null);

        if (monitor == null){
            System.out.println(
                "Monitor not found: " + task.monitorId()
            );
            return;
        }

        checkWebsite(monitor);
    }


    private void checkWebsite(Monitor monitor) {
        
        //Turns our monitor timeout bound into a Duration object
        Duration timeout = Duration.ofSeconds(monitor.getTimeoutSeconds());
        
        //builds a setting which uses the duration timeout so that our request is terminated if it exceeds it
        HttpClientSettings settings = HttpClientSettings.defaults().withTimeouts(timeout, timeout);

        //sets the setting to the request
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.detect().build(settings);

        RestClient restClient = restClientBuilder.clone().requestFactory(requestFactory).build();

        long start = System.nanoTime();
        try {
            
            int statusCode = restClient
                    .get()
                    .uri(monitor.getUrl())
                    .header("User-Agent", "PulseWatch/0.1")
                    .exchange((request, response) ->
                                response.getStatusCode().value()
                );
                
            long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

            System.out.println(
                "Checked " + monitor.getUrl()
                + " status=" + statusCode
                + " latencyMs=" + latencyMs

            );

        } catch (RestClientException e) {

            long latencyMs = TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - start
            );

            System.out.println(
                "Check failed " + monitor.getUrl()
                + " latencyMs=" + latencyMs
                + " error=" + e.getClass().getSimpleName()
            );
        }
    }
}