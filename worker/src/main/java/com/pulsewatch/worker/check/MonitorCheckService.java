package com.pulsewatch.worker.check;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.pulsewatch.common.domain.CheckResult;
import com.pulsewatch.common.domain.Monitor;
import com.pulsewatch.common.messaging.CheckTask;
import com.pulsewatch.persistence.repository.CheckResultRepository;
import com.pulsewatch.persistence.repository.MonitorRepository;


@Service
public class MonitorCheckService {

    private final MonitorRepository monitorRepository;
    private final RestClient.Builder restClientBuilder;
    private final CheckResultRepository checkResultRepository;

    public MonitorCheckService(
            MonitorRepository monitorRepository,
            CheckResultRepository checkResultRepository,
            RestClient.Builder restClientBuilder) {

        this.monitorRepository = monitorRepository;
        this.restClientBuilder = restClientBuilder;
        this.checkResultRepository = checkResultRepository;
    }

    /**
     * Gets the monitor associtated with the given task monitor Id and sends a request
     * @param task The message object for the current task
     */
    public void process(CheckTask task) {


        if (checkResultRepository.existsByTaskId(task.taskId())) {
            System.out.println(
                "Task already processed: " + task.taskId()
            );
            return;
        }

        Monitor monitor = monitorRepository
                          .findById(task.monitorId())
                          .orElse(null);

        if (monitor == null){
            System.out.println(
                "Monitor not found: " + task.monitorId()
            );
            return;
        }

        checkWebsite(task, monitor);
    }


private void checkWebsite(CheckTask task, Monitor monitor) {

    /*
     * Read the timeout configured for this particular Monitor.
     *
     * Example:
     * monitor.getTimeoutSeconds() == 5
     *
     * Duration.ofSeconds(5)
     * creates a Java Duration representing 5 seconds.
     */
    Duration timeout =
            Duration.ofSeconds(monitor.getTimeoutSeconds());


    /*
     * Create HTTP client settings.
     *
     * withTimeouts(timeout, timeout)
     *
     * The first timeout is used for establishing the connection.
     * The second timeout is used while waiting for data from the server.
     *
     * Important:
     * This does NOT strictly mean:
     *
     *     "the entire request can never exceed 5 seconds"
     *
     * because these are separate network timeout stages.
     */
    HttpClientSettings settings =
            HttpClientSettings.defaults()
                    .withTimeouts(timeout, timeout);


    /*
     * Build the actual HTTP request factory.
     *
     * RestClient needs something underneath it that knows how to
     * physically make HTTP requests.
     *
     * Think:
     *
     * RestClient
     *     ↓
     * ClientHttpRequestFactory
     *     ↓
     * actual network connection
     *
     * detect()
     * tells Spring Boot to choose an appropriate HTTP implementation
     * available in the application.
     *
     * build(settings)
     * creates it using the timeout settings we just created.
     */
    ClientHttpRequestFactory requestFactory =
            ClientHttpRequestFactoryBuilder
                    .detect()
                    .build(settings);


    /*
     * restClientBuilder was injected into this service by Spring.
     *
     * clone()
     * gives us a copy of the builder so that configuring this Monitor's
     * timeout doesn't modify the shared builder.
     *
     * requestFactory(requestFactory)
     * tells this RestClient to use the request factory we configured above.
     *
     * build()
     * creates the actual RestClient we are going to use.
     */
    RestClient restClient =
            restClientBuilder
                    .clone()
                    .requestFactory(requestFactory)
                    .build();


    /*
     * Record our starting point BEFORE making the HTTP request.
     *
     * System.nanoTime() is useful for measuring elapsed time.
     *
     * We're going to calculate:
     *
     * end time - start time = request latency
     *
     * This is different from Instant.now().
     *
     * Instant.now()
     *     → "What time did something happen?"
     *
     * System.nanoTime()
     *     → "How long did something take?"
     */
    long start = System.nanoTime();


    try {

        /*
         * Start building an HTTP GET request.
         *
         * Example:
         *
         * GET https://google.com
         */
        int statusCode = restClient

                /*
                 * Specify the HTTP method.
                 *
                 * We're currently using GET because we want to actually
                 * request the monitored resource.
                 */
                .get()

                /*
                 * Set the destination URL.
                 *
                 * Example:
                 *
                 * monitor.getUrl()
                 * → "https://google.com"
                 */
                .uri(monitor.getUrl())

                /*
                 * Add a User-Agent HTTP header.
                 *
                 * Instead of appearing as some unidentified Java HTTP client,
                 * PulseWatch identifies itself.
                 *
                 * Request might look roughly like:
                 *
                 * GET / HTTP/1.1
                 * Host: google.com
                 * User-Agent: PulseWatch/0.1
                 */
                .header("User-Agent", "PulseWatch/0.1")

                /*
                 * Actually perform the request.
                 *
                 * exchange() gives us access to the HTTP response.
                 *
                 * The lambda receives:
                 *
                 * request
                 *     → information about the outgoing request
                 *
                 * response
                 *     → the HTTP response received from the website
                 *
                 * We currently don't need anything from `request`.
                 */
                .exchange((request, response) ->

                        /*
                         * response.getStatusCode()
                         *
                         * might represent:
                         *
                         * 200 OK
                         * 404 NOT FOUND
                         * 500 INTERNAL SERVER ERROR
                         * 503 SERVICE UNAVAILABLE
                         *
                         * .value()
                         * converts that status into its integer representation.
                         *
                         * Example:
                         *
                         * response.getStatusCode().value()
                         * → 200
                         */
                        response.getStatusCode().value()
                );


        /*
         * The HTTP request has now completed successfully enough
         * for us to receive an HTTP response.
         *
         * Get the current timer value and subtract the starting value.
         */
        long latencyMs = TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - start
        );

        CheckResult result = new CheckResult(
            task.taskId(), 
            monitor, 
            Instant.now(),
            statusCode,
            latencyMs,
            null
        );

        checkResultRepository.save(result);

        /*
         * Print our temporary monitoring result.
         *
         * Example:
         *
         * Checked https://google.com status=200 latencyMs=142
         *
         * Eventually we will NOT just print this.
         *
         * We will create a CheckResult and store:
         *
         * statusCode = 200
         * latencyMs = 142
         * error = null
         */
        System.out.println(
                "Checked " + monitor.getUrl()
                + " status=" + statusCode
                + " latencyMs=" + latencyMs
        );


    } catch (RestClientException e) {

        /*
         * We reach this block when the RestClient encounters an HTTP-client
         * level failure rather than receiving a normal response that we
         * successfully process.
         *
         * Possible examples include things such as:
         *
         * timeout
         * connection failure
         * DNS/network problem
         *
         * We'll classify those more precisely later.
         */


        /*
         * Even failed attempts have latency.
         *
         * Suppose our timeout is 5 seconds:
         *
         * start
         *   ↓
         * wait...
         *   ↓
         * timeout
         *
         * latencyMs might be approximately 5000.
         *
         * That's useful monitoring information, so we still record it.
         */
        long latencyMs = TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - start
        );


        /*
         * Print information about the failed check.
         *
         * e.getClass().getSimpleName()
         *
         * returns the Java exception class name without its package.
         *
         * Example:
         *
         * org.springframework.web.client.ResourceAccessException
         *
         * becomes:
         *
         * ResourceAccessException
         *
         * Later we will translate exceptions into our own CheckError enum:
         *
         * TIMEOUT
         * DNS_ERROR
         * CONNECTION_REFUSED
         * TLS_ERROR
         * NETWORK_ERROR
         */
        System.out.println(
                "Check failed " + monitor.getUrl()
                + " latencyMs=" + latencyMs
                + " error=" + e.getClass().getSimpleName()
        );
    }

    }
}