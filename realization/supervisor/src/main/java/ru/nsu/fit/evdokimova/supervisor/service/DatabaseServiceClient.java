package ru.nsu.fit.evdokimova.supervisor.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.nsu.fit.evdokimova.supervisor.model.ModelRequest;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DatabaseServiceClient {
    private final RestTemplate restTemplate;
    private final String DATABASE_SERVICE_URL = "http://localhost:8081/api";

    public List<ModelRequest> getModelsByExperiment(Long experimentId) {
        String url = DATABASE_SERVICE_URL + "/models/experiment/" + experimentId;
        return List.of(restTemplate.getForObject(url, ModelRequest[].class));
    }
}
