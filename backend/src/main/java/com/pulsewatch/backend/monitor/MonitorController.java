package com.pulsewatch.backend.monitor;

import java.net.URI;

import org.springframework.http.ResponseEntity;
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
}