package ru.nsu.fit.evdokimova.supervisor.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class ModelRequest {
    private String modelId;
    private String name;
    private Integer order;
    private String version;
    private ModelLanguage language;
    private String modelPath;
//    private List<String> parametersName;
    private Map<String, String> parameters = new HashMap<>();
}