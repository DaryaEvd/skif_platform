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
    void bindsOnlyEnergyAndRejectsConflictingStaticValues() {
        var factory = new StartJsonFactory(objectMapper, model -> Map.of("E_start",30,"E_end",30,"t",1));
        var model = new ModelRequest();
        model.setLanguage(ru.nsu.fit.evdokimova.supervisor.model.ModelLanguage.C);
        model.setParameters(Map.of("t",2));
        var result = factory.create(model,Map.of("E_start",31.5,"E_end",31.5,"unrelated",42));
        assertThat(result).containsEntry("E_start",31.5).containsEntry("E_end",31.5).containsEntry("t",2);
        assertThat(result).doesNotContainKey("unrelated");
        model.setParameters(Map.of("E_start",31.5));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> factory.create(model,Map.of("E_start",31.5,"E_end",31.5)))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("conflicts");
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
