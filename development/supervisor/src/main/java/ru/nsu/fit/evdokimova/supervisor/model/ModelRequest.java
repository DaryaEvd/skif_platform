package ru.nsu.fit.evdokimova.supervisor.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.nio.file.Path;
import java.util.HashMap;
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
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
    @io.swagger.v3.oas.annotations.media.Schema(type = "string", example = "model_01")
    private Path modelPath;

    private Map<String, Object> parameters = new HashMap<>();
}