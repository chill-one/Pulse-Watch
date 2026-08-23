package com.pulsewatch.backend.monitor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import com.pulsewatch.common.domain.CheckResult;
import com.pulsewatch.common.domain.CheckError;
import com.pulsewatch.common.domain.Incident;
import com.pulsewatch.common.domain.Monitor;
import com.pulsewatch.common.domain.MonitorStatus;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MonitorController.class)
@ContextConfiguration(classes = MonitorController.class)
class MonitorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MonitorService monitorService;

    @Test
    void createsMonitorAndReturnsLocation() throws Exception {
        UUID id = UUID.randomUUID();
        Monitor monitor = monitor(id, MonitorStatus.PENDING);
        when(monitorService.createMonitor(any(CreateMonitorRequest.class))).thenReturn(monitor);

        mockMvc.perform(post("/monitors")
                        .contentType("application/json")
                        .content("{\"name\":\"Example\",\"url\":\"https://example.test\",\"checkIntervalSeconds\":60,\"timeoutSeconds\":10}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/monitors/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void rejectsInvalidCreateRequest() throws Exception {
        mockMvc.perform(post("/monitors")
                        .contentType("application/json")
                        .content("{\"name\":\"Example\",\"url\":\"ftp://example.test\",\"checkIntervalSeconds\":60,\"timeoutSeconds\":10}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getsMonitorOrReturnsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(monitorService.getMointor(id)).thenReturn(Optional.of(monitor(id, MonitorStatus.UP)));

        mockMvc.perform(get("/monitors/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Example"));

        UUID missing = UUID.randomUUID();
        when(monitorService.getMointor(missing)).thenReturn(Optional.empty());
        mockMvc.perform(get("/monitors/{id}", missing))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatesAndDeletesMonitor() throws Exception {
        UUID id = UUID.randomUUID();
        Monitor monitor = monitor(id, MonitorStatus.UP);
        when(monitorService.updateMonitor(eq(id), any(UpdateMonitorRequest.class)))
                .thenReturn(Optional.of(monitor));
        when(monitorService.deleteByMonitor(id)).thenReturn(true);

        mockMvc.perform(patch("/monitors/{id}", id)
                        .contentType("application/json")
                        .content("{\"name\":\"Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));

        mockMvc.perform(delete("/monitors/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void mapsRecentChecksAndIncidents() throws Exception {
        UUID id = UUID.randomUUID();
        Monitor monitor = monitor(id, MonitorStatus.UP);
        CheckResult check = new CheckResult(
                UUID.randomUUID(), monitor, Instant.parse("2026-01-01T00:00:00Z"),
                200, 42, null);
        Incident incident = new Incident(monitor, Instant.parse("2026-01-01T00:00:00Z"));
        incident.resolve(Instant.parse("2026-01-01T00:01:00Z"));
        when(monitorService.getRecentChecks(id, 5)).thenReturn(Optional.of(List.of(check)));
        when(monitorService.getRecentIncidents(id, 5)).thenReturn(Optional.of(List.of(incident)));

        mockMvc.perform(get("/monitors/{id}/checks?limit=5", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].statusCode").value(200))
                .andExpect(jsonPath("$[0].latencyMs").value(42));
        mockMvc.perform(get("/monitors/{id}/incidents?limit=5", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].durationSeconds").value(60));
    }

    private static Monitor monitor(UUID id, MonitorStatus status) {
        Monitor monitor = new Monitor(
                "Example", "https://example.test", 60, 10,
                Instant.parse("2026-01-01T00:01:00Z"), status,
                status == MonitorStatus.DOWN ? 3 : 0,
                Instant.parse("2026-01-01T00:00:00Z"));
        org.springframework.test.util.ReflectionTestUtils.setField(monitor, "id", id);
        return monitor;
    }
}
