package ru.nsu.fit.evdokimova.supervisor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.nsu.fit.evdokimova.supervisor.model.ModelRequest;
import ru.nsu.fit.evdokimova.supervisor.service.createparams.IModelDefaultsProvider;
import ru.nsu.fit.evdokimova.supervisor.service.execmodels.PythonModelExecutor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Component
public class StartJsonFactory {
    private static final Logger log = LoggerFactory.getLogger(PythonModelExecutor.class);

    private final ObjectMapper objectMapper;
    private final IModelDefaultsProvider defaultsProvider;

    public Map<String, Object> create(
            ModelRequest model,
            Map<String, Object> previousOutput
    ) {
        Map<String, Object> result = new HashMap<>();

        Map<String, Object>defaultParameters =defaultsProvider.getDefaultParameters(model);

        result.putAll(defaultParameters);

        if (previousOutput != null) {
            result.putAll(previousOutput);
        }

        result.putAll(model.getParameters());

        log.info(
                "Created start data for model named {} with amount of params = {}",
                model.getName(),
                result.size()
        );

        return result;
    }

    public Path writeStartFile(
            Path startFile,
            Map<String, Object> data
    ) throws IOException {

        Files.createDirectories(startFile.getParent());
        log.info("Writing start.json to {}", startFile.toAbsolutePath());
        Files.writeString(
                startFile,
                objectMapper.writeValueAsString(data)
        );

        return startFile;
    }
}
