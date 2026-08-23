package com.pulsewatch.worker.check;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import org.springframework.beans.factory.annotation.Autowired;

import com.pulsewatch.common.domain.Alert;
import com.pulsewatch.common.domain.AlertType;
import com.pulsewatch.common.domain.CheckError;
import com.pulsewatch.common.domain.DeliveryStatus;
import com.pulsewatch.common.domain.Incident;
import com.pulsewatch.common.domain.Monitor;
import com.pulsewatch.common.domain.MonitorStatus;
import com.pulsewatch.common.messaging.CheckTask;
import com.pulsewatch.persistence.repository.AlertRepository;
import com.pulsewatch.persistence.repository.CheckResultRepository;
import com.pulsewatch.persistence.repository.IncidentRepository;
import com.pulsewatch.persistence.repository.MonitorRepository;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ContextConfiguration(classes = CheckPersistenceServiceIT.JpaTestConfiguration.class)
@Import(CheckPersistenceService.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers
class CheckPersistenceServiceIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private MonitorRepository monitorRepository;

    @Autowired
    private CheckResultRepository checkResultRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private CheckPersistenceService service;

    @Test
    void protectsFullOutageAndRecoveryLifecycle() {
        Monitor monitor = monitorRepository.saveAndFlush(new Monitor(
                "lifecycle", "https://example.test", 60, 10,
                Instant.parse("2026-01-01T00:01:00Z"), MonitorStatus.PENDING, 0,
                Instant.parse("2026-01-01T00:00:00Z")));
        Instant base = Instant.parse("2026-01-01T00:00:00Z");

        record(monitor, base.plusSeconds(1), 500, null);
        record(monitor, base.plusSeconds(2), 503, null);
        record(monitor, base.plusSeconds(3), 503, null);

        assertThat(monitor.getStatus()).isEqualTo(MonitorStatus.DOWN);
        assertThat(monitor.getConsecutiveFailureCount()).isEqualTo(3);
        List<Incident> incidents = incidentRepository.findByMonitorOrderByStartedAtDesc(
                monitor, org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(incidents).hasSize(1);
        Incident incident = incidents.getFirst();
        assertThat(incident.getEndedAt()).isNull();
        assertThat(alertRepository.findTop50ByDeliveryStatusOrderByCreatedAtAsc(DeliveryStatus.PENDING))
                .extracting(Alert::getType)
                .containsExactly(AlertType.OUTAGE);

        record(monitor, base.plusSeconds(4), 502, null);
        assertThat(incidentRepository.findByMonitorOrderByStartedAtDesc(
                monitor, org.springframework.data.domain.PageRequest.of(0, 10))).hasSize(1);
        assertThat(alertRepository.findTop50ByDeliveryStatusOrderByCreatedAtAsc(DeliveryStatus.PENDING))
                .extracting(Alert::getType)
                .containsExactly(AlertType.OUTAGE);

        record(monitor, base.plusSeconds(5), 200, null);

        assertThat(monitor.getStatus()).isEqualTo(MonitorStatus.UP);
        assertThat(monitor.getConsecutiveFailureCount()).isZero();
        assertThat(incident.getEndedAt()).isEqualTo(base.plusSeconds(5));
        assertThat(alertRepository.findTop50ByDeliveryStatusOrderByCreatedAtAsc(DeliveryStatus.PENDING))
                .extracting(Alert::getType)
                .containsExactly(AlertType.OUTAGE, AlertType.RECOVERY);
        assertThat(checkResultRepository.findByMonitorOrderByCheckedAtDesc(
                monitor, org.springframework.data.domain.PageRequest.of(0, 10))).hasSize(5);
    }

    @Test
    void persistsNetworkErrorAsFailureAndIgnoresDuplicateTask() {
        Monitor monitor = monitorRepository.saveAndFlush(new Monitor(
                "network", "https://example.test", 60, 10, Instant.now(),
                MonitorStatus.UP, 0, Instant.now()));
        UUID taskId = UUID.randomUUID();
        CheckTask task = new CheckTask(taskId, monitor.getId(), Instant.now());

        service.recordResult(task, Instant.now(), null, 1000, CheckError.DNS_ERROR);
        service.recordResult(task, Instant.now().plusSeconds(1), 200, 10, null);

        assertThat(checkResultRepository.existsByTaskId(taskId)).isTrue();
        assertThat(checkResultRepository.findByMonitorOrderByCheckedAtDesc(
                monitor, org.springframework.data.domain.PageRequest.of(0, 10))).hasSize(1);
        assertThat(monitor.getStatus()).isEqualTo(MonitorStatus.DEGRADED);
        assertThat(monitor.getConsecutiveFailureCount()).isOne();
    }

    @Test
    void missingMonitorFailsWithoutCreatingCheckResult() {
        UUID monitorId = UUID.randomUUID();
        CheckTask task = new CheckTask(UUID.randomUUID(), monitorId, Instant.now());

        assertThatThrownBy(() -> service.recordResult(task, Instant.now(), 200, 1, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Monitor not found: " + monitorId);
        assertThat(checkResultRepository.findAll()).isEmpty();
    }

    private void record(Monitor monitor, Instant checkedAt, Integer statusCode, CheckError error) {
        service.recordResult(
                new CheckTask(UUID.randomUUID(), monitor.getId(), checkedAt),
                checkedAt,
                statusCode,
                25,
                error);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableJpaRepositories(basePackages = "com.pulsewatch.persistence.repository")
    @EntityScan(basePackages = "com.pulsewatch.common.domain")
    static class JpaTestConfiguration {
    }
}
