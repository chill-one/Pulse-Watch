package com.pulsewatch.backend.monitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.pulsewatch.common.domain.Monitor;
import com.pulsewatch.common.domain.MonitorStatus;
import com.pulsewatch.persistence.repository.AlertRepository;
import com.pulsewatch.persistence.repository.CheckResultRepository;
import com.pulsewatch.persistence.repository.IncidentRepository;
import com.pulsewatch.persistence.repository.MonitorRepository;

@ExtendWith(MockitoExtension.class)
class MonitorServiceTest {

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private CheckResultRepository checkResultRepository;

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private AlertRepository alertRepository;

    @Test
    void createsPendingMonitorWithNextCheckAfterInterval() {
        CreateMonitorRequest request = new CreateMonitorRequest(
                "Example",
                "https://example.test",
                60,
                10
        );
        when(monitorRepository.save(any(Monitor.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        MonitorService service = service();
        Instant before = Instant.now();

        Monitor result = service.createMonitor(request);

        Instant after = Instant.now();
        assertThat(result.getName()).isEqualTo("Example");
        assertThat(result.getUrl()).isEqualTo("https://example.test");
        assertThat(result.getCheckIntervalSeconds()).isEqualTo(60);
        assertThat(result.getTimeoutSeconds()).isEqualTo(10);
        assertThat(result.getStatus()).isEqualTo(MonitorStatus.PENDING);
        assertThat(result.getConsecutiveFailureCount()).isZero();
        assertThat(result.getCreatedAt()).isBetween(before, after);
        assertThat(result.getNextCheckAt())
                .isBetween(before.plusSeconds(60), after.plusSeconds(60));
        verify(monitorRepository).save(result);
    }

    @Test
    void delegatesMonitorRetrieval() {
        UUID id = UUID.randomUUID();
        Monitor monitor = monitor(id, MonitorStatus.UP);
        when(monitorRepository.findAll()).thenReturn(List.of(monitor));
        when(monitorRepository.findById(id)).thenReturn(Optional.of(monitor));
        MonitorService service = service();

        assertThat(service.getAllMointors()).containsExactly(monitor);
        assertThat(service.getMointor(id)).contains(monitor);
    }

    @Test
    void returnsEmptyForMissingMonitorHistory() {
        UUID id = UUID.randomUUID();
        when(monitorRepository.findById(id)).thenReturn(Optional.empty());
        MonitorService service = service();

        assertThat(service.getRecentChecks(id, 20)).isEmpty();
        assertThat(service.getRecentIncidents(id, 20)).isEmpty();
        verify(checkResultRepository, never())
                .findByMonitorOrderByCheckedAtDesc(any(), any());
        verify(incidentRepository, never())
                .findByMonitorOrderByStartedAtDesc(any(), any());
    }

    @Test
    void clampsHistoryLimitsToOneThroughOneHundred() {
        UUID id = UUID.randomUUID();
        Monitor monitor = monitor(id, MonitorStatus.UP);
        when(monitorRepository.findById(id)).thenReturn(Optional.of(monitor));
        when(checkResultRepository.findByMonitorOrderByCheckedAtDesc(any(), any()))
                .thenReturn(List.of());
        when(incidentRepository.findByMonitorOrderByStartedAtDesc(any(), any()))
                .thenReturn(List.of());
        MonitorService service = service();

        service.getRecentChecks(id, -10);
        service.getRecentChecks(id, 1000);
        service.getRecentIncidents(id, 0);
        service.getRecentIncidents(id, 101);

        ArgumentCaptor<PageRequest> checkPages = ArgumentCaptor.forClass(PageRequest.class);
        verify(checkResultRepository, org.mockito.Mockito.times(2))
                .findByMonitorOrderByCheckedAtDesc(any(), checkPages.capture());
        assertThat(checkPages.getAllValues())
                .extracting(PageRequest::getPageSize)
                .containsExactly(1, 100);

        ArgumentCaptor<PageRequest> incidentPages = ArgumentCaptor.forClass(PageRequest.class);
        verify(incidentRepository, org.mockito.Mockito.times(2))
                .findByMonitorOrderByStartedAtDesc(any(), incidentPages.capture());
        assertThat(incidentPages.getAllValues())
                .extracting(PageRequest::getPageSize)
                .containsExactly(1, 100);
    }

    @Test
    void updatesConfigurationPartiallyAndReschedulesOnlyWhenIntervalChanges() {
        UUID id = UUID.randomUUID();
        Instant originalNextCheck = Instant.parse("2026-01-01T00:01:00Z");
        Monitor monitor = new Monitor(
                "old",
                "https://old.example",
                60,
                10,
                originalNextCheck,
                MonitorStatus.DOWN,
                3,
                Instant.parse("2026-01-01T00:00:00Z")
        );
        when(monitorRepository.findById(id)).thenReturn(Optional.of(monitor));
        MonitorService service = service();

        Instant before = Instant.now();
        Optional<Monitor> updated = service.updateMonitor(
                id,
                new UpdateMonitorRequest("new", null, 120, null)
        );

        assertThat(updated).contains(monitor);
        assertThat(monitor.getName()).isEqualTo("new");
        assertThat(monitor.getUrl()).isEqualTo("https://old.example");
        assertThat(monitor.getCheckIntervalSeconds()).isEqualTo(120);
        assertThat(monitor.getTimeoutSeconds()).isEqualTo(10);
        assertThat(monitor.getStatus()).isEqualTo(MonitorStatus.DOWN);
        assertThat(monitor.getNextCheckAt())
                .isBetween(before.plusSeconds(120), Instant.now().plusSeconds(120));
        verify(monitorRepository, never()).save(any(Monitor.class));
    }

    @Test
    void updateLeavesScheduleUntouchedWhenIntervalIsNotChanged() {
        UUID id = UUID.randomUUID();
        Monitor monitor = monitor(id, MonitorStatus.UP);
        Instant originalNextCheck = monitor.getNextCheckAt();
        when(monitorRepository.findById(id)).thenReturn(Optional.of(monitor));

        service().updateMonitor(id, new UpdateMonitorRequest(null, "https://new.example", null, 20));

        assertThat(monitor.getUrl()).isEqualTo("https://new.example");
        assertThat(monitor.getNextCheckAt()).isEqualTo(originalNextCheck);
    }

    @Test
    void updateAndDeleteReturnEmptyOrFalseWhenMonitorIsMissing() {
        UUID id = UUID.randomUUID();
        when(monitorRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(service().updateMonitor(id, new UpdateMonitorRequest(null, null, null, null)))
                .isEmpty();
        assertThat(service().deleteByMonitor(id)).isFalse();
        verify(monitorRepository, never()).delete(any(Monitor.class));
    }

    @Test
    void deletesRelatedDataBeforeMonitor() {
        UUID id = UUID.randomUUID();
        Monitor monitor = monitor(id, MonitorStatus.UP);
        when(monitorRepository.findById(id)).thenReturn(Optional.of(monitor));
        MonitorService service = service();

        assertThat(service.deleteByMonitor(id)).isTrue();

        InOrder order = inOrder(alertRepository, incidentRepository, checkResultRepository, monitorRepository);
        order.verify(alertRepository).deleteByIncidentMonitor(monitor);
        order.verify(incidentRepository).deleteByMonitor(monitor);
        order.verify(checkResultRepository).deleteByMonitor(monitor);
        order.verify(monitorRepository).delete(monitor);
    }

    private MonitorService service() {
        return new MonitorService(
                monitorRepository,
                checkResultRepository,
                incidentRepository,
                alertRepository
        );
    }

    private static Monitor monitor(UUID id, MonitorStatus status) {
        Monitor monitor = new Monitor(
                "example",
                "https://example.test",
                60,
                10,
                Instant.parse("2026-01-01T00:01:00Z"),
                status,
                status == MonitorStatus.DOWN ? 3 : 0,
                Instant.parse("2026-01-01T00:00:00Z")
        );
        org.springframework.test.util.ReflectionTestUtils.setField(monitor, "id", id);
        return monitor;
    }
}
