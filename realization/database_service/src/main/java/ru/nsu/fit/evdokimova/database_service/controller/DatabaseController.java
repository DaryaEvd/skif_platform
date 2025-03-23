package ru.nsu.fit.evdokimova.database_service.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.nsu.fit.evdokimova.database_service.dto.RequestExperimentFromClient;
import ru.nsu.fit.evdokimova.database_service.dto.StartJsonDto;
import ru.nsu.fit.evdokimova.database_service.repository.StartJsonRepository;
import ru.nsu.fit.evdokimova.database_service.service.ExperimentService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/database")
public class DatabaseController {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseController.class);

    private final ExperimentService experimentService;
    private final StartJsonRepository startJsonRepository;

    @PostMapping("/process-experiment")
    public ResponseEntity<String> processExperiment(@RequestBody RequestExperimentFromClient request) {
        try {
            logger.info("Received experiment request: {}", request);
            experimentService.processExperiment(request);
            return ResponseEntity.ok("Experiment processed");
        } catch (Exception e) {
            logger.error("Error processing experiment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/get-start-files/{experimentId}")
    public ResponseEntity<List<StartJsonDto>> getStartFiles(@PathVariable Long experimentId) {
        List<StartJsonDto> startFiles = startJsonRepository.findByExperimentId(experimentId);
        return ResponseEntity.ok(startFiles);
    }
}
