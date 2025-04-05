package ru.nsu.fit.evdokimova.supervisor.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.nsu.fit.evdokimova.supervisor.model.ExperimentResult;
import ru.nsu.fit.evdokimova.supervisor.model.ModelRequest;
import ru.nsu.fit.evdokimova.supervisor.model.RequestExperimentFromClient;
import ru.nsu.fit.evdokimova.supervisor.model.StartJsonDto;
import ru.nsu.fit.evdokimova.supervisor.utils.LocalModelLoader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static ru.nsu.fit.evdokimova.supervisor.utils.Constants.END_JSONS_PATH;
import static ru.nsu.fit.evdokimova.supervisor.utils.Constants.START_JSONS_PATH;

@AllArgsConstructor
@Service
public class ExperimentService {
    private static final Logger logger = LoggerFactory.getLogger(ExperimentService.class);

    private final DockerService dockerService;
    private final LocalModelLoader modelLoader; // todo: сделать с интерфейсом потом норм

    private final Map<Long, List<StartJsonDto>> experimentStartFiles = new ConcurrentHashMap<>();

    private final Map<Long, ExperimentResult> experimentResultsMap = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

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

        ExperimentResult experimentResult = new ExperimentResult();
        experimentResult.setExperimentId(request.getExperimentId());
        experimentResult.setExperimentName(request.getExperimentName());
        experimentResultsMap.put(request.getExperimentId(), experimentResult);

        for (ModelRequest model : sortedModels) {
            StartJsonDto currentJson = findStartJsonForModel(startJsons, model.getOrder());

            Path startJsonFilePath = saveModelInputJson(currentJson, model.getOrder());
            logger.info("curr json is {}", startJsonFilePath);

            String containerId = dockerService.buildAndRunModel(model);
            logger.info("container with ID {} has finished", containerId);

            dockerService.waitForContainerCompletion(containerId);
            processModelOutput(model, currentJson, startJsons);

            updateExperimentResult(request.getExperimentId(), model.getOrder());
        }

        calculateFinalResult(request.getExperimentId());
    }

    private void updateExperimentResult(Long experimentId, int modelOrder) {
        try {
            Path outputPath = Path.of(END_JSONS_PATH, String.format("end%d.json", modelOrder));
            JsonNode resultNode = new ObjectMapper().readTree(outputPath.toFile());

            if (resultNode.has("result")) {
                double result = resultNode.get("result").asDouble();
                ExperimentResult expResult = experimentResultsMap.get(experimentId);
                expResult.getPartialResults().put("model" + modelOrder, result);
            }
        } catch (IOException e) {
            logger.error("Failed to update experiment result", e);
        }
    }

    private void calculateFinalResult(Long experimentId) {
        ExperimentResult result = experimentResultsMap.get(experimentId);

        try {
            Double term1 = result.getPartialResults().get("model1");
            Double term2 = result.getPartialResults().get("model2");
            Double term3 = result.getPartialResults().get("model3");

            if (term1 == null || term2 == null || term3 == null) {
                throw new IllegalStateException("Not all partial results available");
            }

            double finalValue = term1 + term2 + term3;
            result.setFinalResult(finalValue);
            result.setCalculationTime(LocalDateTime.now());

            saveFinalResult(experimentId);

            logger.info("Calculated final result for experiment {}: {}",
                    experimentId, result);

        } catch (Exception e) {
            logger.error("Failed to calculate final result for experiment {}", experimentId, e);
            throw new RuntimeException("Final result calculation failed", e);
        }
    }

    private void saveFinalResult(Long experimentId) {
        try {
            Path resultPath = Path.of(END_JSONS_PATH,
                    String.format("experiment_%d_result.json", experimentId));

            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            objectMapper.writeValue(resultPath.toFile(),
                    experimentResultsMap.get(experimentId));
        } catch (IOException e) {
            logger.error("Failed to save final experiment result", e);
            throw new RuntimeException("Failed to save experiment result", e);
        }
    }

    public ExperimentResult getExperimentResult(Long experimentId) {
        return experimentResultsMap.get(experimentId);
    }

    private List<StartJsonDto> createInitialStartJsons(RequestExperimentFromClient request) {
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
                    new TypeReference<>() {
                    });

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

    private String generateParameterValue(String paramName) {
        return switch (paramName) {
            case "X_0" -> "3";
            case "V_0" -> "5";
            case "t" -> "7";
            case "a" -> "2";
            default -> "0";
        };
    }

    private Path saveModelInputJson(StartJsonDto startJson, int order) {
        try {
            String filename = String.format("start%d.json", order);
            Path dirPath = Path.of(START_JSONS_PATH);

            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            Path filePath = dirPath.resolve(filename);
            ObjectMapper objectMapper = new ObjectMapper();

            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

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