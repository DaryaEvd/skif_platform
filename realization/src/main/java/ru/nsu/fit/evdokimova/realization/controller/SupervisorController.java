package ru.nsu.fit.evdokimova.realization.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.nsu.fit.evdokimova.realization.model.RequestExperimentFromClient;
import ru.nsu.fit.evdokimova.realization.service.ExperimentService;

@RestController
@RequestMapping("/api/experiments")
public class SupervisorController {
    private static final Logger logger = LoggerFactory.getLogger(SupervisorController.class);
    private final ExperimentService experimentService;

    public SupervisorController(ExperimentService experimentService) {
        this.experimentService = experimentService;
    }

    @PostMapping("/start")
    public ResponseEntity<String>  startExperiment(@RequestBody RequestExperimentFromClient request) {
        try {
            request.generateId();
            experimentService.validateExperiment(request);
            logger.info("Received request for starting experiment {}", request);
            return ResponseEntity.ok("Experiment: '" + request.getExperimentId() + "' is in processing");
        } catch (IllegalArgumentException e) {
            logger.error("Error validation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        }
    }
}
