package ru.nsu.fit.evdokimova.supervisor.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.nsu.fit.evdokimova.supervisor.model.RequestExperimentFromClient;
import ru.nsu.fit.evdokimova.supervisor.model.StartJsonDto;
import ru.nsu.fit.evdokimova.supervisor.service.ExperimentService;

import java.util.Collections;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/experiments")
public class SupervisorController {
    private static final Logger logger = LoggerFactory.getLogger(SupervisorController.class);
    private final ExperimentService experimentService;

    @PostMapping("/start")
    public ResponseEntity<String> startExperiment(@RequestBody RequestExperimentFromClient request) {
        request.generateId();
        logger.info("Received experiment: {}", request.getExperimentId());

        experimentService.processExperiment(request);

        return ResponseEntity.ok(String.format("Experiment '%s' started.", request.getExperimentId()));
    }

    @GetMapping("/show-files/{experimentId}")
    public ResponseEntity<List<StartJsonDto>> getStartFiles(@PathVariable Long experimentId) {
        List<StartJsonDto> files = experimentService.getStartFiles(experimentId);
        if (files.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.emptyList());
        }
        return ResponseEntity.ok(files);
    }
}