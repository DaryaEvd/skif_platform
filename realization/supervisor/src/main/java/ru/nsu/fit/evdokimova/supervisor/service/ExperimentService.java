package ru.nsu.fit.evdokimova.supervisor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import ru.nsu.fit.evdokimova.supervisor.model.ModelRequest;
import ru.nsu.fit.evdokimova.supervisor.model.RequestExperimentFromClient;
import ru.nsu.fit.evdokimova.supervisor.model.StartJsonDto;

import java.util.Arrays;
import java.util.List;

@Service
public class ExperimentService {
    private static final Logger logger = LoggerFactory.getLogger(ExperimentService.class);

    private final RestTemplate restTemplate;
    public ExperimentService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<StartJsonDto> fetchStartFiles(Long experimentId) {
        String url = "http://database_service:8081/api/database/get-start-files/" + experimentId;
        ResponseEntity<StartJsonDto[]> response = restTemplate.getForEntity(url, StartJsonDto[].class);
        return Arrays.asList(response.getBody());
    }

    public void sendExperimentToDatabase(RequestExperimentFromClient request) {
        String url = "http://database_service:8081/api/database/process-experiment";
        try {
            restTemplate.postForEntity(url, request, Void.class);
            logger.info("Experiment '{}' sent to database_service for processing.", request.getExperimentId());
        } catch (Exception e) {
            logger.error("Failed to send experiment '{}' to database_service: {}", request.getExperimentId(), e.getMessage());
        }
    }

    public void validateExperiment(RequestExperimentFromClient request) {
        if (!StringUtils.hasText(request.getExperimentName())) {
            throw new IllegalArgumentException("Experiment name can't be empty");
        }
        if (request.getModels() == null || request.getModels().isEmpty()) {
            throw new IllegalArgumentException("Experiment should have at least one model");
        }

        for (ModelRequest model : request.getModels()) {
            if (!StringUtils.hasText(model.getName())) {
                throw new IllegalArgumentException("Model name can't be empty");
            }
            if (model.getOrder() == null) {
                throw new IllegalArgumentException("Model " + model.getName() + "should have an order");
            }
            if (!StringUtils.hasText(model.getVersion())) {
                throw new IllegalArgumentException("Model " + model.getName() + "should have a version");
            }
            if (model.getParametersName() == null || model.getParametersName().isEmpty()) {
                throw new IllegalArgumentException("Model " + model.getName() + "should have input parameters");
            }
        }
    }
}
