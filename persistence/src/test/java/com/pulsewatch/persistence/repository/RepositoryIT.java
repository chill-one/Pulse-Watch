package com.pulsewatch.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.pulsewatch.common.domain.Alert;
import com.pulsewatch.common.domain.AlertType;
import com.pulsewatch.common.domain.CheckResult;
import com.pulsewatch.common.domain.DeliveryStatus;
import com.pulsewatch.common.domain.Incident;
import com.pulsewatch.common.domain.Monitor;
import com.pulsewatch.common.domain.MonitorStatus;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ContextConfiguration(classes = RepositoryIT.JpaTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers
class RepositoryIT {

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
    private EntityManager entityManager;

    @Test
    void findsDueMonitorsAtOrBeforeNowInOldestFirstOrder() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        Monitor oldest = monitor("oldest", now.minusSeconds(120), now.minusSeconds(10));
        Monitor exact = monitor("exact", now.minusSeconds(60), now);
        Monitor future = monitor("future", now.minusSeconds(30), now.plusSeconds(1));
        monitorRepository.saveAllAndFlush(List.of(future, exact, oldest));

        List<Monitor> due = monitorRepository
                .findTop200ByNextCheckAtLessThanEqualOrderByNextCheckAtAsc(now);

        assertThat(due).containsExactly(oldest, exact);
    }

    @Test
    void findsRecentChecksInDescendingOrderWithPageLimit() {
        Monitor monitor = savedMonitor("history");
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        CheckResult oldest = check(monitor, base, 200);
        CheckResult middle = check(monitor, base.plusSeconds(1), 204);
        CheckResult newest = check(monitor, base.plusSeconds(2), 503);
        checkResultRepository.saveAllAndFlush(List.of(oldest, middle, newest));

        List<CheckResult> recent = checkResultRepository
                .findByMonitorOrderByCheckedAtDesc(monitor, PageRequest.of(0, 2));

        assertThat(recent).containsExactly(newest, middle);
        assertThat(checkResultRepository.existsByTaskId(newest.getTaskId())).isTrue();
    }

    @Test
    void enforcesUniqueTaskIdAtDatabaseBoundary() {
        Monitor monitor = savedMonitor("unique-task");
        UUID taskId = UUID.randomUUID();
        checkResultRepository.saveAndFlush(check(monitor, Instant.now(), 200, taskId));

        assertThatThrownBy(() -> checkResultRepository.saveAndFlush(
                check(monitor, Instant.now().plusSeconds(1), 200, taskId)))
                .isInstanceOfAny(RuntimeException.class);
    }

    @Test
    void findsNewestOpenIncidentAndOrdersIncidentHistory() {
        Monitor monitor = savedMonitor("incidents");
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        Incident resolved = new Incident(monitor, base);
        resolved.resolve(base.plusSeconds(10));
        Incident oldOpen = new Incident(monitor, base.plusSeconds(20));
        Incident newestOpen = new Incident(monitor, base.plusSeconds(30));
        incidentRepository.saveAllAndFlush(List.of(resolved, oldOpen, newestOpen));

        assertThat(incidentRepository
                .findFirstByMonitorAndEndedAtIsNullOrderByStartedAtDesc(monitor))
                .contains(newestOpen);
        assertThat(incidentRepository.findByMonitorOrderByStartedAtDesc(
                monitor, PageRequest.of(0, 2)))
                .containsExactly(newestOpen, oldOpen);
    }

    @Test
    void findsPendingAlertsOldestFirstAndExcludesOtherStatuses() {
        Monitor monitor = savedMonitor("alerts");
        Incident incident = incidentRepository.saveAndFlush(
                new Incident(monitor, Instant.parse("2026-01-01T00:00:00Z")));
        Alert oldest = new Alert(incident, AlertType.OUTAGE,
                Instant.parse("2026-01-01T00:00:01Z"));
        Alert newest = new Alert(incident, AlertType.RECOVERY,
                Instant.parse("2026-01-01T00:00:03Z"));
        Alert sent = new Alert(incident, AlertType.OUTAGE,
                Instant.parse("2026-01-01T00:00:02Z"));
        sent.markSent(Instant.parse("2026-01-01T00:00:04Z"));
        Alert failed = new Alert(incident, AlertType.OUTAGE,
                Instant.parse("2026-01-01T00:00:05Z"));
        failed.markFailed();
        alertRepository.saveAllAndFlush(List.of(newest, failed, sent, oldest));

        assertThat(alertRepository.findTop50ByDeliveryStatusOrderByCreatedAtAsc(DeliveryStatus.PENDING))
                .containsExactly(oldest, newest);
    }

    @Test
    void relationshipsPersistAndRepositoryDeletesRemoveMonitorHistory() {
        Monitor monitor = savedMonitor("deletion");
        Incident incident = incidentRepository.saveAndFlush(
                new Incident(monitor, Instant.parse("2026-01-01T00:00:00Z")));
        CheckResult result = checkResultRepository.saveAndFlush(
                check(monitor, Instant.parse("2026-01-01T00:00:01Z"), 200));
        Alert alert = alertRepository.saveAndFlush(
                new Alert(incident, AlertType.OUTAGE,
                        Instant.parse("2026-01-01T00:00:02Z")));
        entityManager.clear();

        Monitor reloaded = monitorRepository.findById(monitor.getId()).orElseThrow();
        assertThat(checkResultRepository.findById(result.getId()).orElseThrow().getMonitor().getId())
                .isEqualTo(reloaded.getId());
        assertThat(alertRepository.findById(alert.getId()).orElseThrow().getIncident().getMonitor().getId())
                .isEqualTo(reloaded.getId());

        alertRepository.deleteByIncidentMonitor(reloaded);
        incidentRepository.deleteByMonitor(reloaded);
        checkResultRepository.deleteByMonitor(reloaded);
        entityManager.flush();

        assertThat(alertRepository.findAll()).isEmpty();
        assertThat(incidentRepository.findAll()).isEmpty();
        assertThat(checkResultRepository.findAll()).isEmpty();
        assertThat(monitorRepository.findById(reloaded.getId())).isPresent();
    }

    private Monitor savedMonitor(String name) {
        return monitorRepository.saveAndFlush(monitor(name, Instant.now(), Instant.now()));
    }

    private static Monitor monitor(String name, Instant createdAt, Instant nextCheckAt) {
        return new Monitor(name, "https://example.test", 60, 10,
                nextCheckAt, MonitorStatus.PENDING, 0, createdAt);
    }

    private static CheckResult check(Monitor monitor, Instant checkedAt, int status) {
        return check(monitor, checkedAt, status, UUID.randomUUID());
    }

    private static CheckResult check(Monitor monitor, Instant checkedAt, int status, UUID taskId) {
        return new CheckResult(taskId, monitor, checkedAt, status, 20, null);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableJpaRepositories(basePackages = "com.pulsewatch.persistence.repository")
    @EntityScan(basePackages = "com.pulsewatch.common.domain")
    static class JpaTestConfiguration {
    }
}
