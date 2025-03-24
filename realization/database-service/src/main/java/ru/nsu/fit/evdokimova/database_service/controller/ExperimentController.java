package ru.nsu.fit.evdokimova.database_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.nsu.fit.evdokimova.database_service.dto.ExperimentDto;
import ru.nsu.fit.evdokimova.database_service.repository.ExperimentRepository;

import java.util.List;

@RestController
@RequestMapping("/api/experiments")
@RequiredArgsConstructor
public class ExperimentController {
    private final ExperimentRepository experimentRepository;

    @GetMapping("/{experimentId}")
    public ResponseEntity<ExperimentDto> getExperiment(@PathVariable Long experimentId) {
        return experimentRepository.findById(experimentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<ExperimentDto>> getAllExperiments() {
        return ResponseEntity.ok(experimentRepository.findAll());
    }
}