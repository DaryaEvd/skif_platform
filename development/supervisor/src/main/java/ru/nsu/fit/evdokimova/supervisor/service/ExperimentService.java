package ru.nsu.fit.evdokimova.supervisor.service;

import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.nsu.fit.evdokimova.supervisor.model.ExperimentStatus;
import ru.nsu.fit.evdokimova.supervisor.model.ModelRequest;
import ru.nsu.fit.evdokimova.supervisor.model.RequestExperimentFromClient;
import ru.nsu.fit.evdokimova.supervisor.model.StartJsonDto;
import ru.nsu.fit.evdokimova.supervisor.utils.IModelLoader;
import ru.nsu.fit.evdokimova.supervisor.utils.LocalModelLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static ru.nsu.fit.evdokimova.supervisor.utils.Constants.*;

@AllArgsConstructor
@Service
public class ExperimentService {
    private final Map<Long, List<StartJsonDto>> storedFiles = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(ExperimentService.class);

//    private final Map<Long, ExperimentStatus> experimentStatuses = new ConcurrentHashMap<>();

    private final DockerService dockerService;
    private final LocalModelLoader modelLoader; // todo: сделать с интерфейсом потом норм

    private final Map<Long, List<StartJsonDto>> experimentStartFiles = new ConcurrentHashMap<>();

    public List<StartJsonDto> getStartFiles(Long experimentId) {
        return experimentStartFiles.getOrDefault(experimentId, Collections.emptyList());
    }

    private void saveStartFiles(Long experimentId, List<StartJsonDto> files) {
        experimentStartFiles.put(experimentId, files);
    }

    public void processExperiment(RequestExperimentFromClient request) throws Exception {
        List<ModelRequest> sortedModels = request.getModels().stream()
                .sorted(Comparator.comparingInt(ModelRequest::getOrder))
                .toList();

        List<StartJsonDto> startJsons = createInitialStartJsons(request);
        saveStartFiles(request.getExperimentId(), startJsons);

        for (ModelRequest model : sortedModels) {
            StartJsonDto currentJson = findStartJsonForModel(startJsons, model.getOrder());

            Path startJsonFilePath = saveModelInputJson(currentJson, model.getOrder());
            logger.info("curr json is {}", startJsonFilePath);

            String containerId = dockerService.buildAndRunModel(model);
            logger.info("container with ID {} has finished", containerId);

            dockerService.waitForContainerCompletion(containerId);
            processModelOutput(model, currentJson, startJsons);
        }
    }

    private List<StartJsonDto> createInitialStartJsons(RequestExperimentFromClient request) {
//        return request.getModels().stream()
//                .map(model -> {
//                    StartJsonDto dto = new StartJsonDto();
//                    dto.setExperimentId(request.getExperimentId());
//                    dto.setModelId(model.getModelId());
//                    dto.setModelName(model.getName());
//                    dto.setOrder(model.getOrder());
//                    dto.setVersion(model.getVersion());
//                    dto.setLanguage(model.getLanguage());
//                    dto.setExperimentName(request.getExperimentName());
//                    dto.setParameters(generateParameters(model.getParametersName()));
//                    dto.setModelPath(model.getModelPath());
//                    return dto;
//                })
//                .toList();

        return request.getModels().stream()
                .map(model -> new StartJsonDto(
                        request.getExperimentId(),
                        model.getModelId(),
                        model.getName(),
                        model.getOrder(),
                        model.getVersion(),
                        model.getLanguage(),
                        request.getExperimentName(),
                        generateInitialParameters(model.getParametersName()),
                        new HashMap<>(),
                        model.getModelPath()
                ))
                .toList();
    }

    private Map<String, String> generateInitialParameters(List<String> paramNames) {
        Map<String, String> params = new HashMap<>();
        for (String param : paramNames) {
            // Разделяем параметры, если они переданы через запятую
            String[] individualParams = param.split(",\\s*");
            for (String p : individualParams) {
                params.put(p, generateParameterValue(p));
            }
        }
        return params;
    }

    private StartJsonDto findStartJsonForModel(List<StartJsonDto> startJsons, int order) {
        return startJsons.stream()
                .filter(json -> json.getOrder() == order)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No start JSON found for model with order: " + order));
    }

    private void processModelOutput(ModelRequest model, StartJsonDto currentJson,
                                    List<StartJsonDto> allStartJsons) {
        try {
            Path outputPath = Path.of(END_JSONS_PATH, String.format("end%d.json", model.getOrder()));

            if (!Files.exists(outputPath)) {
                throw new FileNotFoundException("Output file not found: " + outputPath);
            }

            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> results = mapper.readValue(outputPath.toFile(),
                    new TypeReference<Map<String, String>>() {});

            if (model.getOrder() < allStartJsons.size()) {
                StartJsonDto nextJson = findStartJsonForModel(allStartJsons, model.getOrder() + 1);

                results.forEach((key, value) -> {
                    nextJson.getPreviousResults().put(model.getName() + "_" + key, value);
                });

                nextJson.getParameters().forEach((paramName, oldValue) -> {
                    if (results.containsKey(paramName)) {
                        nextJson.getParameters().put(paramName, results.get(paramName));
                    }
                });

                saveModelInputJson(nextJson, nextJson.getOrder());
            }

            logger.info("Processed output for model {}: {} results added",
                    model.getName(), results.size());

        } catch (Exception e) {
            logger.error("Failed to process output for model {}", model.getName(), e);
            throw new RuntimeException("Output processing failed", e);
        }
    }

    private Map<String, String> parseResults(JsonNode resultNode) {
        Map<String, String> results = new HashMap<>();

        if (resultNode.isObject()) {
            resultNode.fields().forEachRemaining(entry -> {
                if (entry.getValue().isValueNode()) {
                    results.put(entry.getKey(), entry.getValue().asText());
                }
            });
        }

        return results;
    }

    private Map<String, String> generateParameters(List<String> paramNames) {
        Map<String, String> params = new HashMap<>();
        paramNames.forEach(name -> params.put(name, generateParameterValue(name)));
        return params;
    }

    private String generateParameterValue(String paramName) {
        return switch (paramName) {
            case "X_0" -> "3";
            case "V_0" -> "5";
            case "t" -> "7";
            case "a" -> "2";
            default -> "0";
        };
    }

//    public ExperimentStatus getExperimentStatus(Long experimentId) {
//        return experimentStatuses.getOrDefault(experimentId,
//                new ExperimentStatus(experimentId, null, 0, 0, "NOT_FOUND", LocalDateTime.now(), Map.of()));
//    }

//    private void updateExperimentStatus(Long experimentId, ModelRequest model, String status) {
//        ExperimentStatus expStatus = experimentStatuses.computeIfAbsent(experimentId,
//                id -> new ExperimentStatus(id, null, 0, 0, "RUNNING", LocalDateTime.now(), new HashMap<>()));
//
//        expStatus.setCurrentModel(model.getName());
//        expStatus.setCurrentOrder(model.getOrder());
//        expStatus.setLastUpdated(LocalDateTime.now());
//        expStatus.setStatus(status);
//
//        ExperimentStatus.ModelStatus modelStatus = expStatus.getModelStatuses().computeIfAbsent(model.getOrder(),
//                order -> new ExperimentStatus.ModelStatus());
//
//        modelStatus.setModelName(model.getName());
//        modelStatus.setStatus(status);
//        if ("RUNNING".equals(status)) {
//            modelStatus.setStartTime(LocalDateTime.now());
//        } else if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
//            modelStatus.setEndTime(LocalDateTime.now());
//        }
//    }

    private Path saveModelInputJson(StartJsonDto startJson, int order) {
        try {
            String filename = String.format("start%d.json", order);
            Path dirPath = Path.of(START_JSONS_PATH);

            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            Path filePath = dirPath.resolve(filename);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(filePath.toFile(), startJson);

            logger.info("Saved input JSON for model {} (order {}) at {}",
                    startJson.getModelName(), order, filePath);

            return filePath;
        } catch (IOException e) {
            logger.error("Failed to save input JSON for model {} (order {})",
                    startJson.getModelName(), order, e);
            throw new RuntimeException("Failed to save input JSON", e);
        }
    }
}