package ru.nsu.fit.evdokimova.supervisor.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExperimentResult {
    private Long experimentId;
    private String experimentName;
    private Map<String, Double> partialResults = new HashMap<>();
    private Double finalResult;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime calculationTime;

    private String formula = "X = X_0 * 2 + V_0 * t + (a * (t^2) / 2)";

    @Override
    public String toString() {
        return String.format("Experiment %d '%s'\nFormula: %s\nPartial results: %s\nFinal result: %.2f\nCalculated at: %s",
                experimentId, experimentName, formula, partialResults, finalResult, calculationTime);
    }
}