package com.pulsewatch.backend.monitor;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pulsewatch.common.domain.Monitor;

import jakarta.validation.Valid;



@RestController
// @RequestMapping -> defines the base path
@RequestMapping("/monitors")
public class MonitorController {

    private final MonitorService monitorService;

    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    //@ Valid -> Validates the request if invalid 400 response is sent
    //@ RequestBody -> Spring takes JSON from the HTTP Body and converts it into CreateMonitorRequest
    //@PostMapping -> means that this method handles POST /monitors
    @PostMapping
    public ResponseEntity<MonitorResponse> createMonitor(@Valid @RequestBody CreateMonitorRequest request) {

        Monitor monitor = monitorService.createMonitor(request);

        MonitorResponse response = MonitorResponse.from(monitor);

        URI location = URI.create("/monitors/" + monitor.getId());

        //HTTP 201 created and sets a location header to the newly created resources 
        return ResponseEntity
                .created(location)
                .body(response);
    }

    //@GetMapping -> means that this method handles Get /monitors
    @GetMapping
    public List<MonitorResponse> getMointor() {
        // getAllMonitors()
        //      -> gets all Monitor objects from the repository
        //
        // stream()
        //      -> lets us process each Monitor one at a time
        //
        // map(MonitorResponse::from)
        //      -> converts each Monitor into a MonitorResponse
        //
        // toList()
        //      -> collects all converted MonitorResponse objects into a List
        return monitorService.getAllMointors()
                             .stream()
                             .map(MonitorResponse::from)
                             .toList();

    }
    
    //@PathVariable /monitors/550e8400-e29b-41d4-a716-446655440000
    //                       └──────────────────────────────────┘
    //                                         id -> converst it into UUID

    @GetMapping("/{id}")
    public ResponseEntity<MonitorResponse> getMonitor(@PathVariable UUID id) {
        return monitorService.getMointor(id)
                             .map(MonitorResponse::from)
                             .map(ResponseEntity::ok)
                             .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    
}