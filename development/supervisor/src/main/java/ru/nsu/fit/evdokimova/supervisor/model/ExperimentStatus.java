package ru.nsu.fit.evdokimova.supervisor.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentStatus {
    private Long experimentId;
    private String currentModel;
    private int currentOrder;
    private int totalModels;
    private String status; // "RUNNING", "COMPLETED", "FAILED"
    private LocalDateTime lastUpdated;
    private Map<Integer, ModelStatus> modelStatuses;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelStatus {
        private String modelName;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String errorMessage;
    }
}