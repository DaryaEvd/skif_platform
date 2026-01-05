package ru.nsu.fit.evdokimova.supervisor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.nsu.fit.evdokimova.supervisor.configuration.MainPaths;
import ru.nsu.fit.evdokimova.supervisor.model.ModelRequest;
import ru.nsu.fit.evdokimova.supervisor.model.RequestExperimentFromClient;
import ru.nsu.fit.evdokimova.supervisor.service.execmodels.ModelExecutor;
import ru.nsu.fit.evdokimova.supervisor.service.execmodels.ModelExecutorRegistry;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Service
public class ExperimentService {

    private static final Logger log = LoggerFactory.getLogger(ExperimentService.class);

    private final StartJsonFactory startJsonFactory;
    private final ModelExecutorRegistry executorRegistry;
    private final ObjectMapper objectMapper;
    private final MainPaths mainPaths;

    public void processExperiment(RequestExperimentFromClient request) throws Exception {

        log.info("Starting experiment {}", request.getExperimentName());

        List<ModelRequest> models =
                request.getModels().stream()
                        .sorted(Comparator.comparingInt(ModelRequest::getOrder))
                        .toList();

        log.info("models amount is {}", request.getModels().size());

        Map previousOutput = null;

        for (ModelRequest model : models) {

            log.info("Running model named: {} in language: {}", model.getName(), model.getLanguage());

            log.info("model's path in experiment service is: {}", model.getModelPath());

            Path startFile =
//                    "/home/darya/skif_platform/development/supervisor/start_json_files",
                    mainPaths.getStartJsonDirPath().resolve(
                    "start" + model.getOrder() + ".json");

            log.info("startFile is: {}", startFile);

            Path endDir = mainPaths.getEndJsonDirPath();
            log.info("end dir is: {}", endDir);

            Path interModelJsonDir = mainPaths.getInterModelJsonDirPath();
            log.info("intermediate model json dir is: {}", interModelJsonDir);

            Map<String, Object> startData =
                    startJsonFactory.create(model, previousOutput);

            startJsonFactory.writeStartFile(startFile, startData);
            log.info("Start file exists: {}", Files.exists(startFile));
            log.info("Start file size: {}", Files.size(startFile));

            log.info("before writing in exec factory");
            ModelExecutor executor =
                    executorRegistry.get(model.getLanguage());

            Path endJson =
                    executor.execute(model, startFile, endDir, interModelJsonDir);

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