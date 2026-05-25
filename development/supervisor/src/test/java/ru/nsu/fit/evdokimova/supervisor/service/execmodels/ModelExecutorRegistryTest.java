package ru.nsu.fit.evdokimova.supervisor.service.execmodels;

import org.junit.jupiter.api.Test;
import ru.nsu.fit.evdokimova.supervisor.model.ModelLanguage;
import ru.nsu.fit.evdokimova.supervisor.model.ModelRequest;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModelExecutorRegistryTest {

    @Test
    void returnsExecutorThatSupportsRequestedLanguage() {
        ModelExecutor pythonExecutor = executorFor(ModelLanguage.PYTHON);
        ModelExecutor cExecutor = executorFor(ModelLanguage.C);
        ModelExecutorRegistry registry = new ModelExecutorRegistry(List.of(pythonExecutor, cExecutor));

        assertThat(registry.get(ModelLanguage.C)).isSameAs(cExecutor);
    }

    @Test
    void throwsWhenNoExecutorSupportsLanguage() {
        ModelExecutorRegistry registry = new ModelExecutorRegistry(List.of(executorFor(ModelLanguage.PYTHON)));

        assertThatThrownBy(() -> registry.get(ModelLanguage.C))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No executor for model with language C");
    }

    private static ModelExecutor executorFor(ModelLanguage supportedLanguage) {
        return new ModelExecutor() {
            @Override
            public boolean supports(ModelLanguage language) {
                return language == supportedLanguage;
            }

            @Override
            public Path execute(ModelRequest model, Path startFile, Path endDir, Path modelJsonDir) {
                throw new UnsupportedOperationException("Not needed in registry tests");
            }
        };
    }
}
