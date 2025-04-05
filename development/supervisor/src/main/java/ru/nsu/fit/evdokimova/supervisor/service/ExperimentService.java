package ru.nsu.fit.evdokimova.supervisor.service;

import com.fasterxml.jackson.core.ObjectCodec;
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

import static ru.nsu.fit.evdokimova.supervisor.utils.Constants.OUTPUT_PATH_HOST;
import static ru.nsu.fit.evdokimova.supervisor.utils.Constants.START_JSONS_PATH;

@AllArgsConstructor
@Service
public class ExperimentService {
    private final Map<Long, List<StartJsonDto>> storedFiles = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(ExperimentService.class);

    private final Map<Long, ExperimentStatus> experimentStatuses = new ConcurrentHashMap<>();

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


//            waitForContainerCompletion(containerId);
//            processModelOutput(model, currentJson);
        }
    }

    private List<StartJsonDto> createInitialStartJsons(RequestExperimentFromClient request) {
        return request.getModels().stream()
                .map(model -> {
                    StartJsonDto dto = new StartJsonDto();
                    dto.setExperimentId(request.getExperimentId());
                    dto.setModelId(model.getModelId());
                    dto.setModelName(model.getName());
                    dto.setOrder(model.getOrder());
                    dto.setVersion(model.getVersion());
                    dto.setLanguage(model.getLanguage());
                    dto.setExperimentName(request.getExperimentName());
                    dto.setParameters(generateParameters(model.getParametersName()));
                    dto.setModelPath(model.getModelPath());
                    return dto;
                })
                .toList();
    }

    private StartJsonDto findStartJsonForModel(List<StartJsonDto> startJsons, int order) {
        return startJsons.stream()
                .filter(json -> json.getOrder() == order)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No start JSON found for model with order: " + order));
    }

    private void waitForContainerCompletion(String containerId) {
        //  dockerClient.waitContainerCmd(containerId).exec()
    }

//    private void processModelOutput(ModelRequest model, StartJsonDto currentJson) {
//        List<StartJsonDto> startJsons = experimentStartFiles.get(currentJson.getExperimentId());
//        if (startJsons == null) {
//            throw new IllegalStateException("No start files found for experiment " + currentJson.getExperimentId());
//        }
//
//        try {
//            Path outputPath = Path.of(OUTPUT_PATH_HOST,
//                    String.format("end%d.json", model.getOrder()));
//
//            if (!Files.exists(outputPath)) {
//                throw new FileNotFoundException(
//                        "Output file not found at: " + outputPath);
//            }
//
//            logger.info("Output file is  {}",
//                    outputPath);
//
//            ObjectMapper objectMapper = new ObjectMapper();
//            JsonNode resultNode = objectMapper.readTree(outputPath.toFile());
//
//            Map<String, String> results = new HashMap<>();
//            if (resultNode.has("result")) {
//                results.putAll(parseResultNode(resultNode.get("result")));
//            } else {
//                results.putAll(parseResultNode(resultNode));
//            }
//
//            if (model.getOrder() < startJsons.size()) {
//                StartJsonDto nextJson = findStartJsonForModel(
//                        startJsons, model.getOrder() + 1);
//
//                results.forEach((key, value) -> {
//                    String resultKey = model.getName() + "_" + key;
//                    nextJson.addPreviousResult(resultKey, value);
//
//                    if (nextJson.getParameters().containsKey(key)) {
//                        nextJson.getParameters().put(key, value);
//                    }
//                });
//
//                saveModelInputJson(nextJson, nextJson.getOrder());
//            }
//
//            logger.info("Processed output for model {} (order {}): {} results",
//                    model.getName(), model.getOrder(), results.size());
//        } catch (Exception e) {
//            logger.error("Error processing output for model {}", model.getName(), e);
//            throw new RuntimeException("Output processing failed", e);
//        }
//    }
//
//    private Map<String, String> parseResultNode(JsonNode node) {
//        Map<String, String> results = new HashMap<>();
//
//        if (node.isObject()) {
//            node.fields().forEachRemaining(entry -> {
//                if (entry.getValue().isValueNode()) {
//                    results.put(entry.getKey(), entry.getValue().asText());
//                }
//            });
//        } else if (node.isArray()) {
//            for (int i = 0; i < node.size(); i++) {
//                results.put("result_" + i, node.get(i).asText());
//            }
//        } else if (node.isValueNode()) {
//            results.put("result", node.asText());
//        }
//
//        return results;
//    }

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

    public ExperimentStatus getExperimentStatus(Long experimentId) {
        return experimentStatuses.getOrDefault(experimentId,
                new ExperimentStatus(experimentId, null, 0, 0, "NOT_FOUND", LocalDateTime.now(), Map.of()));
    }

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