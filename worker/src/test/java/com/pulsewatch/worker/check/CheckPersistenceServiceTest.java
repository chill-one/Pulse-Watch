package com.pulsewatch.worker.check;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pulsewatch.common.domain.Alert;
import com.pulsewatch.common.domain.AlertType;
import com.pulsewatch.common.domain.CheckError;
import com.pulsewatch.common.domain.CheckResult;
import com.pulsewatch.common.domain.Incident;
import com.pulsewatch.common.domain.Monitor;
import com.pulsewatch.common.domain.MonitorStatus;
import com.pulsewatch.common.messaging.CheckTask;
import com.pulsewatch.persistence.repository.AlertRepository;
import com.pulsewatch.persistence.repository.CheckResultRepository;
import com.pulsewatch.persistence.repository.IncidentRepository;
import com.pulsewatch.persistence.repository.MonitorRepository;

@ExtendWith(MockitoExtension.class)
class CheckPersistenceServiceTest {

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private CheckResultRepository checkResultRepository;

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private AlertRepository alertRepository;

    @Test
    void persistsHealthyResultAndMarksMonitorUp() {
        Monitor monitor = monitor(MonitorStatus.DEGRADED, 2);
        CheckTask task = task();
        when(checkResultRepository.existsByTaskId(task.taskId())).thenReturn(false);
        when(monitorRepository.findById(task.monitorId())).thenReturn(Optional.of(monitor));

        Instant checkedAt = Instant.parse("2026-01-01T00:00:00Z");
        service().recordResult(task, checkedAt, 200, 42, null);

        assertThat(monitor.getStatus()).isEqualTo(MonitorStatus.UP);
        assertThat(monitor.getConsecutiveFailureCount()).isZero();
        ArgumentCaptor<CheckResult> result = ArgumentCaptor.forClass(CheckResult.class);
        verify(checkResultRepository).save(result.capture());
        assertThat(result.getValue().getTaskId()).isEqualTo(task.taskId());
        assertThat(result.getValue().getStatusCode()).isEqualTo(200);
        assertThat(result.getValue().getLatencyMs()).isEqualTo(42);
        assertThat(result.getValue().getError()).isNull();
        verify(monitorRepository).save(monitor);
    }

    @Test
    void treatsErrorAndUnhealthyStatusAsFailure() {
        Monitor monitor = monitor(MonitorStatus.UP, 0);
        CheckTask task = task();
        when(checkResultRepository.existsByTaskId(task.taskId())).thenReturn(false);
        when(monitorRepository.findById(task.monitorId())).thenReturn(Optional.of(monitor));

        service().recordResult(task, Instant.now(), 503, 100, null);

        assertThat(monitor.getStatus()).isEqualTo(MonitorStatus.DEGRADED);
        assertThat(monitor.getConsecutiveFailureCount()).isOne();
        verify(incidentRepository, never()).save(any());
        verify(alertRepository, never()).save(any());

        CheckTask errorTask = task();
        when(checkResultRepository.existsByTaskId(errorTask.taskId())).thenReturn(false);
        when(monitorRepository.findById(errorTask.monitorId())).thenReturn(Optional.of(monitor));
        service().recordResult(errorTask, Instant.now(), null, 1000, CheckError.TIMEOUT);

        assertThat(monitor.getConsecutiveFailureCount()).isEqualTo(2);
        assertThat(monitor.getStatus()).isEqualTo(MonitorStatus.DEGRADED);
    }

    @Test
    void opensOneIncidentAndOutageAlertAtThirdFailure() {
        Monitor monitor = monitor(MonitorStatus.DEGRADED, 2);
        when(incidentRepository.findFirstByMonitorAndEndedAtIsNullOrderByStartedAtDesc(monitor))
                .thenReturn(Optional.empty());
        for (int i = 0; i < 1; i++) {
            CheckTask task = task();
            when(checkResultRepository.existsByTaskId(task.taskId())).thenReturn(false);
            when(monitorRepository.findById(task.monitorId())).thenReturn(Optional.of(monitor));
            service().recordResult(task, Instant.now(), 500, 10, null);
        }

        assertThat(monitor.getStatus()).isEqualTo(MonitorStatus.DOWN);
        assertThat(monitor.getConsecutiveFailureCount()).isEqualTo(3);
        ArgumentCaptor<Incident> incident = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).save(incident.capture());
        assertThat(incident.getValue().getMonitor()).isSameAs(monitor);
        ArgumentCaptor<Alert> alert = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(alert.capture());
        assertThat(alert.getValue().getType()).isEqualTo(AlertType.OUTAGE);
        assertThat(alert.getValue().getIncident()).isSameAs(incident.getValue());
    }

    @Test
    void doesNotOpenDuplicateIncidentForAlreadyDownMonitor() {
        Monitor monitor = monitor(MonitorStatus.DOWN, 3);
        CheckTask task = task();
        when(checkResultRepository.existsByTaskId(task.taskId())).thenReturn(false);
        when(monitorRepository.findById(task.monitorId())).thenReturn(Optional.of(monitor));

        service().recordResult(task, Instant.now(), 503, 10, null);

        assertThat(monitor.getConsecutiveFailureCount()).isEqualTo(4);
        verify(incidentRepository, never()).findFirstByMonitorAndEndedAtIsNullOrderByStartedAtDesc(any());
        verify(incidentRepository, never()).save(any());
        verify(alertRepository, never()).save(any());
    }

    @Test
    void doesNotCreateNewIncidentWhenThresholdIsReachedWithOpenIncident() {
        Monitor monitor = monitor(MonitorStatus.DEGRADED, 2);
        Incident openIncident = new Incident(monitor, Instant.parse("2026-01-01T00:00:00Z"));
        when(incidentRepository.findFirstByMonitorAndEndedAtIsNullOrderByStartedAtDesc(monitor))
                .thenReturn(Optional.of(openIncident));
        CheckTask task = task();
        when(checkResultRepository.existsByTaskId(task.taskId())).thenReturn(false);
        when(monitorRepository.findById(task.monitorId())).thenReturn(Optional.of(monitor));

        service().recordResult(task, Instant.now(), 500, 10, null);

        assertThat(monitor.getStatus()).isEqualTo(MonitorStatus.DOWN);
        verify(incidentRepository, never()).save(any());
        verify(alertRepository, never()).save(any());
    }

    @Test
    void resolvesOpenIncidentAndCreatesRecoveryAlertOnSuccessAfterDown() {
        Monitor monitor = monitor(MonitorStatus.DOWN, 3);
        Incident incident = new Incident(monitor, Instant.parse("2026-01-01T00:00:00Z"));
        when(incidentRepository.findFirstByMonitorAndEndedAtIsNullOrderByStartedAtDesc(monitor))
                .thenReturn(Optional.of(incident));
        CheckTask task = task();
        Instant recoveredAt = Instant.parse("2026-01-01T00:10:00Z");
        when(checkResultRepository.existsByTaskId(task.taskId())).thenReturn(false);
        when(monitorRepository.findById(task.monitorId())).thenReturn(Optional.of(monitor));

        service().recordResult(task, recoveredAt, 204, 12, null);

        assertThat(monitor.getStatus()).isEqualTo(MonitorStatus.UP);
        assertThat(monitor.getConsecutiveFailureCount()).isZero();
        assertThat(incident.getEndedAt()).isEqualTo(recoveredAt);
        verify(incidentRepository).save(incident);
        ArgumentCaptor<Alert> alert = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(alert.capture());
        assertThat(alert.getValue().getType()).isEqualTo(AlertType.RECOVERY);
        assertThat(alert.getValue().getCreatedAt()).isEqualTo(recoveredAt);
    }

    @Test
    void successWhileUpDoesNotLookForOrCreateRecoveryIncident() {
        Monitor monitor = monitor(MonitorStatus.UP, 0);
        CheckTask task = task();
        when(checkResultRepository.existsByTaskId(task.taskId())).thenReturn(false);
        when(monitorRepository.findById(task.monitorId())).thenReturn(Optional.of(monitor));

        service().recordResult(task, Instant.now(), 200, 12, null);

        verify(incidentRepository, never()).findFirstByMonitorAndEndedAtIsNullOrderByStartedAtDesc(any());
        verify(alertRepository, never()).save(any());
    }

    @Test
    void duplicateTaskIsIdempotent() {
        CheckTask task = task();
        when(checkResultRepository.existsByTaskId(task.taskId())).thenReturn(true);

        service().recordResult(task, Instant.now(), 500, 12, null);

        verify(monitorRepository, never()).findById(any());
        verify(checkResultRepository, never()).save(any());
        verify(monitorRepository, never()).save(any());
    }

    @Test
    void missingMonitorFailsBeforeSavingResult() {
        CheckTask task = task();
        when(checkResultRepository.existsByTaskId(task.taskId())).thenReturn(false);
        when(monitorRepository.findById(task.monitorId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().recordResult(task, Instant.now(), 200, 1, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Monitor not found: " + task.monitorId());
        verify(checkResultRepository, never()).save(any());
    }

    private CheckPersistenceService service() {
        return new CheckPersistenceService(
                monitorRepository,
                incidentRepository,
                checkResultRepository,
                alertRepository
        );
    }

    private static CheckTask task() {
        return new CheckTask(UUID.randomUUID(), UUID.randomUUID(), Instant.now());
    }

    private static Monitor monitor(MonitorStatus status, int failures) {
        Monitor monitor = new Monitor(
                "example",
                "https://example.test",
                60,
                10,
                Instant.now(),
                status,
                failures,
                Instant.now());
        UUID id = UUID.randomUUID();
        org.springframework.test.util.ReflectionTestUtils.setField(monitor, "id", id);
        return monitor;
    }
}
