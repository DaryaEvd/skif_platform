package ru.nsu.fit.evdokimova.supervisor.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.nsu.fit.evdokimova.supervisor.model.ExperimentResponse;
import ru.nsu.fit.evdokimova.supervisor.model.RequestExperimentFromClient;
import ru.nsu.fit.evdokimova.supervisor.service.ExperimentService;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/experiments")
public class SupervisorController {
    private static final Logger logger = LoggerFactory.getLogger(SupervisorController.class);
    private final ExperimentService experimentService;

    @PostMapping("/start")
    public ResponseEntity<ExperimentResponse> startExperiment(@RequestBody RequestExperimentFromClient request) {
        try {
            request.generateId();
            logger.info("Received experiment with ID: {}", request.getExperimentId());

            experimentService.processExperiment(request);

            return ResponseEntity.ok(new ExperimentResponse(
                    request.getExperimentId(),
                    "Experiment started successfully",
                    request.getModels().size(),
                    LocalDateTime.now()
            ));
        } catch (Exception e) {
            logger.error("Error starting experiment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ExperimentResponse(null, "Error: " + e.getMessage(), 0, LocalDateTime.now()));
        }
    }
}