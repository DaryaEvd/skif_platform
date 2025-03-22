package ru.nsu.fit.evdokimova.supervisor.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class RequestExperimentFromClient {
    private String experimentId;
    private String experimentName;
    private List<ModelRequest> models;

    public void generateId() {
        this.experimentId = UUID.randomUUID().toString();
    }
}
