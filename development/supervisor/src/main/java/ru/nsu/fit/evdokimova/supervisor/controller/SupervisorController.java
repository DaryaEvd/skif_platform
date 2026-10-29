package ru.nsu.fit.evdokimova.supervisor.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import ru.nsu.fit.evdokimova.supervisor.model.*;
import ru.nsu.fit.evdokimova.supervisor.service.ExperimentService;
import ru.nsu.fit.evdokimova.supervisor.service.runtime.ExperimentFiles;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/experiments")
public class SupervisorController {
    private final ExperimentService experimentService;
    private final ExperimentFiles files;
    @PostMapping("/start")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="202",description="Experiment registered; execution continues in background"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="400",description="Invalid configuration or conflicting energy binding"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="409",description="Another experiment is active")
    })
    @Operation(summary="Запустить эксперимент Python → C",description="Асинхронная регистрация; пример запроса доступен через GET /api/experiments/example и в Try it out. Один активный эксперимент.")
    public ResponseEntity<ExperimentResponse> startExperiment(@RequestBody RequestExperimentFromClient request) throws IOException {
        experimentService.processExperiment(request);
        return ResponseEntity.accepted().body(new ExperimentResponse(request.getExperimentId(),
            "Experiment started successfully",request.getModels().size(),LocalDateTime.now()));
    }
    @GetMapping("/example") @Operation(summary="Получить готовый JSON эксперимента")
    public JsonNode example() throws IOException {
        return files.mapper.readTree(new ClassPathResource("examples/python-c.json").getInputStream());
    }
    @GetMapping("/{id}/status") @Operation(summary="Статус эксперимента и отдельных моделей")
    public ExperimentStatus status(@PathVariable long id) throws IOException {return experimentService.status(id);}
    @PostMapping("/{id}/cancel") @Operation(summary="Отменить эксперимент",description="CANCELLING — команда принята; CANCELLED только после остановки активных вычислений и контейнера.")
    public ResponseEntity<ExperimentStatus> cancel(@PathVariable long id) throws IOException {
        ExperimentStatus status=experimentService.cancel(id);
        return ResponseEntity.status(status.getStatus().equals("CANCELLING")?202:200).body(status);
    }
    @GetMapping("/{id}/configuration") @Operation(summary="Сохранённая конфигурация эксперимента")
    public JsonNode configuration(@PathVariable long id) throws IOException {return experimentService.configuration(id);}
    @GetMapping("/{id}/results") @Operation(summary="Метаданные сохранённых результатов",description="Без большой матрицы; полный результат — отдельным запросом.")
    public List<JsonNode> results(@PathVariable long id) throws IOException {return experimentService.results(id);}
    @GetMapping(value="/{id}/models/{order}/result",produces=MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary="Скачать полный результат модели",description="order — порядок модели (1 Python, 2 C в примере); матрица C выдаётся только здесь.")
    public Resource result(@PathVariable long id,@PathVariable int order) throws IOException {
        return new FileSystemResource(experimentService.modelFile(id,order,"result.json"));
    }
    @GetMapping("/{id}/models/{order}/request") @Operation(summary="Фактический HTTP-запрос к модели")
    public JsonNode request(@PathVariable long id,@PathVariable int order) throws IOException {
        return files.read(experimentService.modelFile(id,order,"request.json"));
    }
    @GetMapping("/{id}/logs") @Operation(summary="Журнал этапов эксперимента")
    public Map<String,Object> logs(@PathVariable long id,@RequestParam(defaultValue="0") int after,@RequestParam(defaultValue="100") int limit) throws IOException {
        return experimentService.logs(id,after,limit);
    }
    @GetMapping(value="/{id}/models/{order}/logs",produces="application/x-ndjson") @Operation(summary="Сохранённые логи вычислительной модели")
    public Resource modelLogs(@PathVariable long id,@PathVariable int order) throws IOException {
        return new FileSystemResource(experimentService.modelFile(id,order,"logs.jsonl"));
    }
    @GetMapping(value="/{id}/models/{order}/publications",produces="application/x-ndjson") @Operation(summary="Сохранённые промежуточные публикации модели")
    public Resource publications(@PathVariable long id,@PathVariable int order) throws IOException {
        return new FileSystemResource(experimentService.modelFile(id,order,"publications.jsonl"));
    }
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<Map<String,String>> httpError(org.springframework.web.server.ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("error",String.valueOf(e.getReason())));
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,String>> invalid(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));
    }
}
