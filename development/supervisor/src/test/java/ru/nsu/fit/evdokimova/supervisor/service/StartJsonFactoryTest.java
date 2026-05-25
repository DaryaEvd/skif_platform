package ru.nsu.fit.evdokimova.supervisor.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.nsu.fit.evdokimova.supervisor.model.ModelRequest;
import ru.nsu.fit.evdokimova.supervisor.service.createparams.IModelDefaultsProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StartJsonFactoryTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createMergesDefaultsPreviousOutputAndRequestParametersInPriorityOrder() {
        IModelDefaultsProvider defaultsProvider = model -> Map.of(
                "defaultOnly", 1,
                "overriddenByPrevious", "default",
                "overriddenByRequest", "default"
        );
        StartJsonFactory factory = new StartJsonFactory(objectMapper, defaultsProvider);

        ModelRequest model = new ModelRequest();
        model.setName("model");
        model.setParameters(Map.of(
                "requestOnly", true,
                "overriddenByRequest", "request"
        ));

        Map<String, Object> result = factory.create(
                model,
                Map.of(
                        "previousOnly", 2,
                        "overriddenByPrevious", "previous",
                        "overriddenByRequest", "previous"
                )
        );

        assertThat(result).containsEntry("defaultOnly", 1);
        assertThat(result).containsEntry("previousOnly", 2);
        assertThat(result).containsEntry("requestOnly", true);
        assertThat(result).containsEntry("overriddenByPrevious", "previous");
        assertThat(result).containsEntry("overriddenByRequest", "request");
    }

    @Test
    void writeStartFileCreatesParentDirectoryAndWritesJson(@TempDir Path tempDir) throws Exception {
        StartJsonFactory factory = new StartJsonFactory(objectMapper, model -> Map.of());
        Path startFile = tempDir.resolve("nested").resolve("start1.json");

        Path result = factory.writeStartFile(startFile, Map.of("E_input", 130, "enabled", true));

        assertThat(result).isEqualTo(startFile);
        assertThat(startFile).exists();

        Map<String, Object> written = objectMapper.readValue(
                Files.readString(startFile),
                new TypeReference<>() {
                }
        );
        assertThat(written).containsEntry("E_input", 130);
        assertThat(written).containsEntry("enabled", true);
    }
}
