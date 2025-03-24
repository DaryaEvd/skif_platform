package ru.nsu.fit.evdokimova.supervisor.service;

import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import ru.nsu.fit.evdokimova.supervisor.model.ModelRequest;
import ru.nsu.fit.evdokimova.supervisor.model.RequestExperimentFromClient;
import ru.nsu.fit.evdokimova.supervisor.model.StartJsonDto;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Service
public class ExperimentService {
    private final Map<Long, List<StartJsonDto>> storedFiles = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(ExperimentService.class);
    private static final String SAVE_PATH = "/home/darya/skif_platform/realization/supervisor/start_json_files/";


    public void processExperiment(RequestExperimentFromClient request) {
        logger.info("Processing experiment: {}", request.getExperimentId());

        List<StartJsonDto> startFiles = new ArrayList<>();
        for (ModelRequest model : request.getModels()) {
            Map<String, String> parameters = new HashMap<>();

            for (String paramName : model.getParametersName()) {
                parameters.put(paramName, generateParameterValue(paramName));
            }

            StartJsonDto startJson = new StartJsonDto(
                    request.getExperimentId(),
                    model.getModelId(),
                    model.getOrder(),
                    model.getVersion(),
                    request.getExperimentName(),
                    parameters
            );

            startFiles.add(startJson);
            saveToFile(startJson);
        }

        storedFiles.put(request.getExperimentId(), startFiles);
        logger.info("Experiment {} processed. {} start.json files created.", request.getExperimentId(), startFiles.size());
    }

    public List<StartJsonDto> getStartFiles(Long experimentId) {
        List<StartJsonDto> files = storedFiles.getOrDefault(experimentId, Collections.emptyList());
        logger.info("Fetching start.json files for experiment {}. Found: {}", experimentId, files.size());
        return files;
    }

    private String generateParameterValue(String paramName) {
        switch (paramName) {
            case "X_0": return "3";
            case "V_0": return "5";
            case "t": return "7";
            case "a": return "2";
            default:
                logger.warn("Unknown parameter: {}. Assigned default value: 0", paramName);
                return "0";
        }
    }

    private void saveToFile(StartJsonDto startJson) {
        try {
            File dir = new File(SAVE_PATH);
            if (!dir.exists()) {
                if (dir.mkdirs()) {
                    logger.info("Created directory: {}", SAVE_PATH);
                } else {
                    logger.error("Failed to create directory: {}", SAVE_PATH);
                    return;
                }
            }

            ObjectMapper mapper = new ObjectMapper();
            String filename = String.format("start%d.json", startJson.getOrder());
            File file = new File(SAVE_PATH + filename);
            mapper.writeValue(file, startJson);

            logger.info("Saved {} at {}", filename, file.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to save start.json for model {} in experiment {}", startJson.getModelId(), startJson.getExperimentId(), e);
        }
    }
}
