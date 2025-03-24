package ru.nsu.fit.evdokimova.supervisor.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StartJsonDto {
    private Long experimentId;
    private String modelId;
    private Integer order;
    private String version;
    private String experimentName;
    private Map<String, String> parameters;
}
