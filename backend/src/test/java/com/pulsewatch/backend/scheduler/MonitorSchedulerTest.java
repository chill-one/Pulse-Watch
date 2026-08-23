package com.pulsewatch.backend.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.pulsewatch.common.domain.Monitor;
import com.pulsewatch.common.domain.MonitorStatus;
import com.pulsewatch.common.messaging.CheckTask;
import com.pulsewatch.common.messaging.RabbitMqNames;
import com.pulsewatch.persistence.repository.MonitorRepository;

@ExtendWith(MockitoExtension.class)
class MonitorSchedulerTest {

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    void doesNothingWhenNoMonitorsAreDue() {
        when(monitorRepository.findTop200ByNextCheckAtLessThanEqualOrderByNextCheckAtAsc(any()))
                .thenReturn(List.of());

        scheduler().pollDueMonitors();

        verify(rabbitTemplate, never()).convertAndSend(
                anyString(), anyString(), any(CheckTask.class));
        verify(monitorRepository, never()).save(any());
    }

    @Test
    void publishesTaskAndSavesMonitorForOneDueMonitor() {
        Instant scheduledAt = Instant.now().minusSeconds(5);
        Monitor monitor = monitor(UUID.randomUUID(), scheduledAt, 60);
        when(monitorRepository.findTop200ByNextCheckAtLessThanEqualOrderByNextCheckAtAsc(any()))
                .thenReturn(List.of(monitor));

        Instant before = Instant.now();
        scheduler().pollDueMonitors();
        Instant after = Instant.now();

        ArgumentCaptor<CheckTask> taskCaptor = ArgumentCaptor.forClass(CheckTask.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMqNames.CHECK_EXCHANGE),
                eq(RabbitMqNames.CHECK_ROUTING_KEY),
                taskCaptor.capture()
        );
        CheckTask task = taskCaptor.getValue();
        assertThat(task.taskId()).isNotNull();
        assertThat(task.monitorId()).isEqualTo(monitor.getId());
        assertThat(task.scheduleCheckAt()).isEqualTo(scheduledAt);
        verify(monitorRepository).save(monitor);

        // Characterization of the current implementation: it advances from poll time.
        assertThat(monitor.getNextCheckAt())
                .isBetween(before.plusSeconds(60), after.plusSeconds(60));
    }

    @Test
    void publishesEachDueMonitorIndependently() {
        Monitor first = monitor(UUID.randomUUID(), Instant.now().minusSeconds(30), 30);
        Monitor second = monitor(UUID.randomUUID(), Instant.now().minusSeconds(10), 120);
        when(monitorRepository.findTop200ByNextCheckAtLessThanEqualOrderByNextCheckAtAsc(any()))
                .thenReturn(List.of(first, second));

        scheduler().pollDueMonitors();

        verify(rabbitTemplate, org.mockito.Mockito.times(2))
                .convertAndSend(eq(RabbitMqNames.CHECK_EXCHANGE), eq(RabbitMqNames.CHECK_ROUTING_KEY), any(CheckTask.class));
        verify(monitorRepository).save(first);
        verify(monitorRepository).save(second);
    }

    @Test
    void overdueBySeveralIntervalsStillAdvancesOnlyOnePollIntervalFromNow() {
        Instant veryOldSchedule = Instant.now().minusSeconds(60 * 10);
        Monitor monitor = monitor(UUID.randomUUID(), veryOldSchedule, 60);
        when(monitorRepository.findTop200ByNextCheckAtLessThanEqualOrderByNextCheckAtAsc(any()))
                .thenReturn(List.of(monitor));

        Instant before = Instant.now();
        scheduler().pollDueMonitors();
        Instant after = Instant.now();

        assertThat(monitor.getNextCheckAt())
                .isBetween(before.plusSeconds(60), after.plusSeconds(60));
        assertThat(monitor.getNextCheckAt()).isAfter(veryOldSchedule.plusSeconds(60));
    }

    @Test
    void doesNotAdvanceMonitorWhenPublishingFails() {
        Instant scheduledAt = Instant.now().minusSeconds(5);
        Monitor monitor = monitor(UUID.randomUUID(), scheduledAt, 60);
        when(monitorRepository.findTop200ByNextCheckAtLessThanEqualOrderByNextCheckAtAsc(any()))
                .thenReturn(List.of(monitor));
        org.mockito.Mockito.doThrow(new IllegalStateException("broker unavailable"))
                .when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(CheckTask.class));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> scheduler().pollDueMonitors())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("broker unavailable");

        assertThat(monitor.getNextCheckAt()).isEqualTo(scheduledAt);
        verify(monitorRepository, never()).save(any());
    }

    private MonitorScheduler scheduler() {
        return new MonitorScheduler(monitorRepository, rabbitTemplate);
    }

    private static Monitor monitor(UUID id, Instant nextCheckAt, int intervalSeconds) {
        Monitor monitor = new Monitor(
                "example",
                "https://example.test",
                intervalSeconds,
                10,
                nextCheckAt,
                MonitorStatus.PENDING,
                0,
                Instant.parse("2026-01-01T00:00:00Z")
        );
        org.springframework.test.util.ReflectionTestUtils.setField(monitor, "id", id);
        return monitor;
    }
}
