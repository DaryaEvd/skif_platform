package ru.nsu.fit.evdokimova.supervisor.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.nsu.fit.evdokimova.supervisor.model.*;
import ru.nsu.fit.evdokimova.supervisor.service.ExperimentService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

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

//    @GetMapping("/show-files/{experimentId}")
//    public ResponseEntity<List<StartJsonDto>> getStartFiles(@PathVariable Long experimentId) {
//        try {
//            List<StartJsonDto> files = experimentService.getStartFiles(experimentId);
//            if (files.isEmpty()) {
//                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.emptyList());
//            }
//            return ResponseEntity.ok(files);
//        } catch (Exception e) {
//            logger.error("Error getting files for experiment {}", experimentId, e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
//        }
//    }

//    @GetMapping("/result/{experimentId}")
//    public ResponseEntity<ExperimentResult> getExperimentResult(
//            @PathVariable Long experimentId) {
//        ExperimentResult result = experimentService.getExperimentResult(experimentId);
//        if (result == null) {
//            return ResponseEntity.notFound().build();
//        }
//        return ResponseEntity.ok(result);
//    }

}