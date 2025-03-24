package ru.nsu.fit.evdokimova.database_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.nsu.fit.evdokimova.database_service.dto.ParameterDto;
import ru.nsu.fit.evdokimova.database_service.repository.ParameterRepository;

import java.util.List;

@RestController
@RequestMapping("/api/parameters")
@RequiredArgsConstructor
public class ParameterController {
    private final ParameterRepository parameterRepository;

    @GetMapping("/model/{modelId}")
    public ResponseEntity<List<ParameterDto>> getParametersByModel(@PathVariable Long modelId) {
        return ResponseEntity.ok(parameterRepository.findByModelId(modelId));
    }
}
