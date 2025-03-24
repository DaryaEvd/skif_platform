package ru.nsu.fit.evdokimova.database_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.nsu.fit.evdokimova.database_service.dto.ModelDto;
import ru.nsu.fit.evdokimova.database_service.repository.ModelRepository;

import java.util.List;

@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class ModelController {
    private final ModelRepository modelRepository;

    @GetMapping("/experiment/{experimentId}")
    public ResponseEntity<List<ModelDto>> getModelsByExperiment(@PathVariable Long experimentId) {
        return ResponseEntity.ok(modelRepository.findByExperimentIdOrderByExecutionOrderAsc(experimentId));
    }
}
