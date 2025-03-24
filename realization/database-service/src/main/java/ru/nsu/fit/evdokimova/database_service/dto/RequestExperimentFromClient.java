package ru.nsu.fit.evdokimova.database_service.dto;

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
    private Long experimentId;
    private String experimentName;
    private List<ModelRequest> models;
}
