package com.pulsewatch.worker.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pulsewatch.common.messaging.CheckTask;
import com.pulsewatch.worker.check.MonitorCheckService;

@ExtendWith(MockitoExtension.class)
class CheckTaskConsumerTest {

    @Mock
    private MonitorCheckService monitorCheckService;

    @Test
    void delegatesCompleteTaskToMonitorCheckService() {
        CheckTask task = new CheckTask(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-01-01T00:00:00Z"));

        new CheckTaskConsumer(monitorCheckService).receive(task);

        verify(monitorCheckService).process(task);
    }

    @Test
    void propagatesServiceFailure() {
        CheckTask task = new CheckTask(UUID.randomUUID(), UUID.randomUUID(), Instant.now());
        RuntimeException failure = new IllegalStateException("check failed");
        org.mockito.Mockito.doThrow(failure).when(monitorCheckService).process(task);

        assertThatThrownBy(() -> new CheckTaskConsumer(monitorCheckService).receive(task))
                .isSameAs(failure);
    }
}
