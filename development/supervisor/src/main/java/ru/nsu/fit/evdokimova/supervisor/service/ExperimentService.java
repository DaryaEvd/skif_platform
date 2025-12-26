package ru.nsu.fit.evdokimova.supervisor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


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

        ModelRequest model = request.getModels().get(0);

        String FIXED_MODEL_PATH = "/home/darya/skif_platform/development/supervisor/models/decc_difract";
        Path modelSourceDir = Paths.get(FIXED_MODEL_PATH).normalize();

        if (!Files.isDirectory(modelSourceDir)) {
            throw new IllegalStateException("Model directory not found: " + modelSourceDir);
        }
        logger.info("Using fixed model dir: {}", modelSourceDir);

        String startJsonContent = objectMapper.writeValueAsString(getHardcodedParams());

        Path startJsonPath = Paths.get(START_JSON_DIR, "start" + ".json");

        Files.createDirectories(startJsonPath.getParent());
        Files.writeString(startJsonPath, startJsonContent);

        logger.info("start.json saved to {}", startJsonPath);

        Thread.sleep(3000);

        logger.info("after sleep");
        if (!Files.isDirectory(modelSourceDir)) {
            throw new IllegalArgumentException("Model dir not found: " + modelSourceDir);
        }
        logger.info("Model source dir: {}", modelSourceDir);

        dockerService.buildAndRunCModel(modelSourceDir, startJsonPath);

        Path containerEndJson = Paths.get(END_JSON_DIR, "end.json");
        Path finalEndJson = Paths.get(END_JSON_DIR, "end" + ".json");

        // todo: здесь с номером модели разобраться при добавлении моделей
        if (Files.exists(containerEndJson)) {
            Files.move(containerEndJson, finalEndJson, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            logger.info("end.json → end{}.json", model.getOrder());
        } else {
            logger.warn("end.json not found after model {} run", model.getName());
        }

        logger.info("Experiment '{}' completed successfully", request.getExperimentName());
    }

    @PostConstruct
    public void init() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private Map<String, Object> getHardcodedParams() {
        Map<String, Object> params = new HashMap<>();
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