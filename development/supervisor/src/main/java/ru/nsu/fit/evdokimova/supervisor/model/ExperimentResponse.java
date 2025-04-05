package ru.nsu.fit.evdokimova.supervisor.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ExperimentResponse {
    private Long experimentId;
    private String message;
    private int modelsCount;
    private LocalDateTime startTime;
}
