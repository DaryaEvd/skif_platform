package ru.nsu.fit.evdokimova.supervisor.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.nsu.fit.evdokimova.supervisor.model.ModelRequest;
import ru.nsu.fit.evdokimova.supervisor.model.RequestExperimentFromClient;

@Service
public class ExperimentService {

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
