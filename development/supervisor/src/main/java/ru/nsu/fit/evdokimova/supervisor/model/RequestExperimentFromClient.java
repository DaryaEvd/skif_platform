package ru.nsu.fit.evdokimova.supervisor.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.nsu.fit.evdokimova.supervisor.utils.GeneratorId;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class RequestExperimentFromClient {
    private Long experimentId;
    private String experimentName;
    private List<ModelRequest> models;

    public void generateId() {
        this.experimentId = GeneratorId.generateId();
    }
}