package ru.nsu.fit.evdokimova.supervisor.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import ru.nsu.fit.evdokimova.supervisor.model.RequestExperimentFromClient;
import ru.nsu.fit.evdokimova.supervisor.model.StartJsonDto;
import ru.nsu.fit.evdokimova.supervisor.service.ExperimentService;

import java.util.Arrays;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/experiments")
public class SupervisorController {
    private static final Logger logger = LoggerFactory.getLogger(SupervisorController.class);
    private final ExperimentService experimentService;

    private final RestTemplate restTemplate;

    @PostMapping("/start")
    public ResponseEntity<String> startExperiment(@RequestBody RequestExperimentFromClient request) {
        try {
            request.generateId();
            experimentService.validateExperiment(request);

            experimentService.sendExperimentToDatabase(request);

            logger.info("Experiment '{}' registered, waiting for processing.", request.getExperimentId());
            return ResponseEntity.ok("Experiment '" + request.getExperimentId() + "' registered.");
        } catch (IllegalArgumentException e) {
            logger.error("Error validation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // todo: here is an error
    /*
org.springframework.web.client.HttpClientErrorException$BadRequest: 400  on GET request for "http://localhost:8081/api/database/get-start-files/null": "{"timestamp":"2025-03-23T15:58:56.476+00:00","status":400,"error":"Bad Request","path":"/api/database/get-start-files/null"}"
     */
    @GetMapping("/get-experiment/{experimentId}")
    public List<StartJsonDto> fetchStartFiles(Long experimentId) {
        String url = "http://localhost:8081/api/database/get-start-files/" + experimentId;
        ResponseEntity<StartJsonDto[]> response = restTemplate.getForEntity(url, StartJsonDto[].class);
        return Arrays.asList(response.getBody());
    }
}
