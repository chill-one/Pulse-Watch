package com.pulsewatch.worker.check;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.UUID;

import javax.net.ssl.SSLException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.pulsewatch.common.domain.CheckError;
import com.pulsewatch.common.domain.Monitor;
import com.pulsewatch.common.domain.MonitorStatus;
import com.pulsewatch.common.messaging.CheckTask;
import com.pulsewatch.persistence.repository.CheckResultRepository;
import com.pulsewatch.persistence.repository.MonitorRepository;

@ExtendWith(MockitoExtension.class)
class MonitorCheckServiceTest {

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private CheckResultRepository checkResultRepository;

    @Mock
    private CheckPersistenceService checkPersistenceService;

    @Test
    void recordsStatusAndLatencyForLocalHttpResponses() throws IOException {
        for (int status : new int[]{200, 304, 404, 500}) {
            checkPersistenceService = mock(CheckPersistenceService.class);
            try (TestHttpServer server = server(status, false, null)) {
                server.start();
                Monitor monitor = monitor("http://127.0.0.1:" + server.getAddress().getPort(), 2);
                CheckTask task = task(monitor);
                when(checkResultRepository.existsByTaskId(task.taskId())).thenReturn(false);
                when(monitorRepository.findById(task.monitorId())).thenReturn(java.util.Optional.of(monitor));

                service(RestClient.builder()).process(task);

                ArgumentCaptor<Integer> statusCaptor = ArgumentCaptor.forClass(Integer.class);
                ArgumentCaptor<Long> latencyCaptor = ArgumentCaptor.forClass(Long.class);
                verify(checkPersistenceService).recordResult(
                        eq(task), any(Instant.class), statusCaptor.capture(), latencyCaptor.capture(), isNull());
                assertThat(statusCaptor.getValue()).isEqualTo(status);
                assertThat(latencyCaptor.getValue()).isGreaterThanOrEqualTo(0L);
            }
        }
    }

    @Test
    void sendsGetWithPulseWatchUserAgent() throws IOException {
        java.util.concurrent.atomic.AtomicReference<String> method =
                new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<String> userAgent =
                new java.util.concurrent.atomic.AtomicReference<>();
        try (TestHttpServer server = server(204, false, exchange -> {
            method.set(exchange.getRequestMethod());
            userAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
        })) {
            server.start();
            Monitor monitor = monitor("http://127.0.0.1:" + server.getAddress().getPort(), 2);
            CheckTask task = task(monitor);
            when(checkResultRepository.existsByTaskId(task.taskId())).thenReturn(false);
            when(monitorRepository.findById(task.monitorId())).thenReturn(java.util.Optional.of(monitor));

            service(RestClient.builder()).process(task);

            assertThat(method).hasValue("GET");
            assertThat(userAgent).hasValue("PulseWatch/0.1");
        }
    }

    @Test
    void classifiesDeterministicTimeoutAndNetworkCauses() {
        assertCauseClassified(new HttpTimeoutException("timeout"), CheckError.TIMEOUT);
        assertCauseClassified(new SocketTimeoutException("timeout"), CheckError.TIMEOUT);
        assertCauseClassified(new UnknownHostException("missing.example"), CheckError.DNS_ERROR);
        assertCauseClassified(new SSLException("bad certificate"), CheckError.TLS_ERROR);
        assertCauseClassified(new ConnectException("refused"), CheckError.CONNECTION_REFUSED);
        assertCauseClassified(new IOException("other network problem"), CheckError.NETWORK_ERROR);
    }

    @Test
    void recordsARealDelayedResponseAsTimeout() throws Exception {
        java.util.concurrent.CountDownLatch requestStarted = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        try (TestHttpServer server = server(200, true, exchange -> {
            requestStarted.countDown();
            try {
                release.await();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        })) {
            server.start();
            Monitor monitor = monitor("http://127.0.0.1:" + server.getAddress().getPort(), 1);
            CheckTask task = task(monitor);
            when(checkResultRepository.existsByTaskId(task.taskId())).thenReturn(false);
            when(monitorRepository.findById(task.monitorId())).thenReturn(java.util.Optional.of(monitor));

            service(RestClient.builder()).process(task);
            release.countDown();

            verify(checkPersistenceService).recordResult(
                    eq(task), any(Instant.class), isNull(), any(Long.class), eq(CheckError.TIMEOUT));
            assertThat(requestStarted.getCount()).isZero();
        } finally {
            release.countDown();
        }
    }

    @Test
    void skipsAlreadyProcessedTaskAndMissingMonitor() {
        CheckTask duplicate = new CheckTask(UUID.randomUUID(), UUID.randomUUID(), Instant.now());
        when(checkResultRepository.existsByTaskId(duplicate.taskId())).thenReturn(true);
        service(RestClient.builder()).process(duplicate);
        verify(monitorRepository, org.mockito.Mockito.never()).findById(any());

        CheckTask missing = new CheckTask(UUID.randomUUID(), UUID.randomUUID(), Instant.now());
        when(checkResultRepository.existsByTaskId(missing.taskId())).thenReturn(false);
        when(monitorRepository.findById(missing.monitorId())).thenReturn(java.util.Optional.empty());
        service(RestClient.builder()).process(missing);
        verify(checkPersistenceService, org.mockito.Mockito.never())
                .recordResult(any(), any(), any(), any(Long.class), any());
    }

    @Test
    void propagatesUnexpectedRuntimeExceptionFromHttpBoundary() {
        RuntimeException failure = new IllegalArgumentException("malformed URL");
        RestClient.Builder builder = mockedBuilderThrowingFromUri(failure);
        Monitor monitor = monitor("not a URL", 1);
        CheckTask task = task(monitor);
        when(checkResultRepository.existsByTaskId(task.taskId())).thenReturn(false);
        when(monitorRepository.findById(task.monitorId())).thenReturn(java.util.Optional.of(monitor));

        assertThatThrownBy(() -> service(builder).process(task)).isSameAs(failure);
        verify(checkPersistenceService, org.mockito.Mockito.never())
                .recordResult(any(), any(), any(), any(Long.class), any());
    }

    private void assertCauseClassified(Throwable cause, CheckError expected) {
        CheckPersistenceService persistence = mock(CheckPersistenceService.class);
        RestClient.Builder builder = mockedBuilderThrowing(new RestClientException("request failed", cause));
        Monitor monitor = monitor("https://example.test", 1);
        CheckTask task = task(monitor);
        when(checkResultRepository.existsByTaskId(task.taskId())).thenReturn(false);
        when(monitorRepository.findById(task.monitorId())).thenReturn(java.util.Optional.of(monitor));

        new MonitorCheckService(monitorRepository, checkResultRepository, persistence, builder).process(task);

        verify(persistence).recordResult(
                eq(task), any(Instant.class), isNull(), any(Long.class), eq(expected));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static RestClient.Builder mockedBuilderThrowing(RestClientException failure) {
        RestClient.Builder builder = mock(RestClient.Builder.class);
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec request = mock(RestClient.RequestHeadersUriSpec.class);
        doReturn(builder).when(builder).clone();
        doReturn(builder).when(builder).requestFactory(any());
        doReturn(restClient).when(builder).build();
        doReturn(request).when(restClient).get();
        doReturn(request).when(request).uri(anyString());
        doReturn(request).when(request).header(anyString(), any(String[].class));
        doThrow(failure).when(request).exchange(any());
        return builder;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static RestClient.Builder mockedBuilderThrowingFromUri(RuntimeException failure) {
        RestClient.Builder builder = mock(RestClient.Builder.class);
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec request = mock(RestClient.RequestHeadersUriSpec.class);
        doReturn(builder).when(builder).clone();
        doReturn(builder).when(builder).requestFactory(any());
        doReturn(restClient).when(builder).build();
        doReturn(request).when(restClient).get();
        doThrow(failure).when(request).uri(anyString());
        return builder;
    }

    private MonitorCheckService service(RestClient.Builder builder) {
        return new MonitorCheckService(
                monitorRepository,
                checkResultRepository,
                checkPersistenceService,
                builder);
    }

    private static Monitor monitor(String url, int timeoutSeconds) {
        Monitor monitor = new Monitor(
                "example", url, 60, timeoutSeconds, Instant.now(),
                MonitorStatus.PENDING, 0, Instant.now());
        org.springframework.test.util.ReflectionTestUtils.setField(monitor, "id", UUID.randomUUID());
        return monitor;
    }

    private static CheckTask task(Monitor monitor) {
        return new CheckTask(monitor.getId(), monitor.getId(), Instant.now());
    }

    private static TestHttpServer server(int status, boolean delay, java.util.function.Consumer<HttpExchange> observer)
            throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        server.setExecutor(executor);
        server.createContext("/", exchange -> {
            if (observer != null) {
                observer.accept(exchange);
            }
            if (!delay && status == 304) {
                exchange.sendResponseHeaders(status, -1);
                exchange.close();
            } else if (!delay) {
                byte[] body = "ok".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status, body.length);
                try (var output = exchange.getResponseBody()) {
                    output.write(body);
                }
            }
            if (delay) {
                exchange.close();
            }
        });
        return new TestHttpServer(server, executor);
    }

    private static final class TestHttpServer implements AutoCloseable {
        private final HttpServer server;
        private final ExecutorService executor;

        private TestHttpServer(HttpServer server, ExecutorService executor) {
            this.server = server;
            this.executor = executor;
        }

        private void start() {
            server.start();
        }

        private InetSocketAddress getAddress() {
            return server.getAddress();
        }

        @Override
        public void close() {
            server.stop(0);
            executor.shutdownNow();
        }
    }
}
