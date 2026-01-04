package ru.nsu.fit.evdokimova.supervisor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.nsu.fit.evdokimova.supervisor.model.ExperimentResult;
import ru.nsu.fit.evdokimova.supervisor.model.ModelRequest;
import ru.nsu.fit.evdokimova.supervisor.model.RequestExperimentFromClient;
import ru.nsu.fit.evdokimova.supervisor.model.StartJsonDto;

import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@AllArgsConstructor
@Service
public class ExperimentService {

    private static final Logger log = LoggerFactory.getLogger(ExperimentService.class);

    private final StartJsonFactory startJsonFactory;
    private final ModelExecutorRegistry executorRegistry;
    private final ObjectMapper objectMapper;

    public void processExperiment(RequestExperimentFromClient request) throws Exception {

        log.info("Starting experiment {}", request.getExperimentName());

        List<ModelRequest> models =
                request.getModels().stream()
                        .sorted(Comparator.comparingInt(ModelRequest::getOrder))
                        .toList();

        log.info("models amount is {}", request.getModels().size());

        Map<String, Object> previousOutput = null;

        for (ModelRequest model : models) {

            log.info("Running model named: {} in language: {}", model.getName(), model.getLanguage());

            log.info("model's path in experiment service is: {}", model.getModelPath());

            Path startFile = Paths.get(
                    "/home/darya/skif_platform/development/supervisor/start_json_files",
//                    request.getExperimentId().toString(),
//                    model.getModelId(),
                    "start" + model.getOrder() + ".json"
            );
            log.info("startFile is: {}", startFile);

            Path endDir = Paths.get(
                    "/home/darya/skif_platform/development/supervisor/end_json_files"
//                    "end" + model.getOrder() + ".json"
//                    ,request.getExperimentId().toString(),
//                    model.getModelId()
            );
            log.info("end dir is: {}", endDir);

            Path modelJsonDir = Paths.get(
                    "/home/darya/skif_platform/development/supervisor/model_json"
//                    ,request.getExperimentId().toString(),
//                    model.getModelId()
            );
            log.info("intermediate model json dir is: {}", modelJsonDir);

            Map<String, Object> startData =
                    startJsonFactory.create(model, previousOutput);

            startJsonFactory.writeStartFile(startFile, startData);
            log.info("Start file exists: {}", Files.exists(startFile));
            log.info("Start file size: {}", Files.size(startFile));

            log.info("before writing in exec factory");
            ModelExecutor executor =
                    executorRegistry.get(model.getLanguage());

            Path endJson =
                    executor.execute(model, startFile, endDir, modelJsonDir);

            previousOutput =
                    objectMapper.readValue(endJson.toFile(), Map.class);

            log.info(
                    "Model {} finished, {} params produced",
                    model.getName(),
                    previousOutput.size()
            );

        }

        log.info("Experiment {} completed", request.getExperimentName());
    }
}


/*
//@AllArgsConstructor
@Service
public class ExperimentService {
    private static final Logger logger = LoggerFactory.getLogger(ExperimentService.class);

    @Value("${supervisor.models.root}")
    private String MODELS_ROOT;

    @Value("${supervisor.start.json.dir}")
    private String START_JSON_DIR;

    @Value("${supervisor.end.json.dir}")
    private String END_JSON_DIR;

    private final DockerService dockerService;
    private final ObjectMapper objectMapper;

    public ExperimentService(DockerService dockerService, ObjectMapper objectMapper) {
        this.dockerService = dockerService;
        this.objectMapper = objectMapper;
    }

    public void processExperiment(RequestExperimentFromClient request) throws Exception {
        logger.info("Starting experiment: {}", request.getExperimentName());

        Integer amountOfModels = request.getModels().size();
        logger.info("Amount of models: {}", amountOfModels);

        for(int i = 0; i < amountOfModels; i++) {
            ModelRequest model = request.getModels().get(i);
            Path modelSourceDir = Paths.get(MODELS_ROOT + "/model_0" + model.getOrder()).normalize();

            if (!Files.isDirectory(modelSourceDir)) {
                throw new IllegalArgumentException("Model dir not found: " + modelSourceDir);
            }
            logger.info("Model source dir: {}", modelSourceDir);

            Map<String, Object> startParams = getHardcodedParams(model.getOrder());
            if (model.getOrder() > 1) {
                Path previousEndJson = Paths.get(END_JSON_DIR, "end" + (model.getOrder() - 1) + ".json");
                if (Files.exists(previousEndJson)) {
                    try {
                        Map previousOutput = objectMapper.readValue(previousEndJson.toFile(), Map.class);
                        logger.info("Merging {} entries from {} into model {} params", previousOutput.size(), previousEndJson, model.getOrder());
                        startParams.putAll(previousOutput);
                    } catch (Exception e) {
                        logger.warn("Failed to read/merge previous output from {}: {}", previousEndJson, e.getMessage());
                    }
                } else {
                    logger.warn("Previous output file {} not found — proceeding with hardcoded params only", previousEndJson);
                }
            }

            String startJsonContent = objectMapper.writeValueAsString(startParams);

            Path startJsonPathForCurrModel = Paths.get(START_JSON_DIR, "start" + model.getOrder() + ".json");

            Files.createDirectories(startJsonPathForCurrModel.getParent());
            Files.writeString(startJsonPathForCurrModel, startJsonContent);

            logger.info("start*.json saved to {}", startJsonPathForCurrModel);

            logger.info("after sleep");

            switch (model.getOrder()){
                case 1:
                    dockerService.buildAndRunPythonModel(modelSourceDir, startJsonPathForCurrModel);
                    break;

                case 2:
                    dockerService.buildAndRunCModel(modelSourceDir, startJsonPathForCurrModel);
                    break;
            }


            Path containerEndJson = Paths.get(END_JSON_DIR, "end" + model.getOrder() + ".json");
            Path finalEndJson = Paths.get(END_JSON_DIR, "end" + model.getOrder() + ".json");

            if (Files.exists(containerEndJson)) {
                Files.move(containerEndJson, finalEndJson, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                logger.info("end.json → end{}.json", model.getOrder());
            } else {
                logger.warn("end.json not found after model {} run", model.getName());
            }

//            startJsonContent = "";
        }


        logger.info("Experiment '{}' completed successfully", request.getExperimentName());
    }

    @PostConstruct
    public void init() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private Map<String, Object> getHardcodedParams(Integer order) {
        Map<String, Object> params = new HashMap<>();
        switch (order) {
            case 1:
                params.put("E_input", 1800);
                params.put("h_y_1", 0.2);
                params.put("h_y_2", 0.2);
                params.put("h_x_1", 0.2);
                params.put("h_x_2", 0.1);
                break;

            case 2:
                params.put("c_x", 0L);
                params.put("c_y", 3950000000L);
                params.put("c_z", 0L);
                params.put("s_x", 0L);
                params.put("s_y", 0L);
                params.put("s_z", 0L);
                params.put("omega", 0.0);
                params.put("kappa", 0.0);
                params.put("phi", 0.0);
                params.put("xSampleSize", 600000L);
                params.put("ySampleSize", 600000L);
                params.put("zSampleSize", 600000L);
                params.put("d_x", 0L);
                params.put("d_y", 55000000L);
                params.put("d_z", 0L);
                params.put("theta", 0.0);
                params.put("beta", 0.0);
                params.put("gammaValue", 0.0);
                params.put("sU", 50000L);
                params.put("sB", 50000L);
                params.put("sR", 50000L);
                params.put("sL", 50000L);
                params.put("E_start", 30.0);
                params.put("E_end", 30.0);
                params.put("t", 10L);
                break;
        }
        return params;
    }

    private final Map<Long, List<StartJsonDto>> experimentStartFiles = new ConcurrentHashMap<>();

    private final Map<Long, ExperimentResult> experimentResultsMap = new ConcurrentHashMap<>();

    public List<StartJsonDto> getStartFiles(Long experimentId) {
        return experimentStartFiles.getOrDefault(experimentId, Collections.emptyList());
    }

    public ExperimentResult getExperimentResult(Long experimentId) {
        return experimentResultsMap.get(experimentId);
    }

}
*/