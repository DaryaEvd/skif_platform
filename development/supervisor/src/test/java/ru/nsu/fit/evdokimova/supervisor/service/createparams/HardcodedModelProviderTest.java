package ru.nsu.fit.evdokimova.supervisor.service.createparams;

import org.junit.jupiter.api.Test;
import ru.nsu.fit.evdokimova.supervisor.model.ModelRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HardcodedModelProviderTest {

    private final HardcodedModelProvider provider = new HardcodedModelProvider();

    @Test
    void returnsDefaultsForFirstModel() {
        ModelRequest model = new ModelRequest();
        model.setOrder(17);
        model.setLanguage(ru.nsu.fit.evdokimova.supervisor.model.ModelLanguage.PYTHON);

        Map<String, Object> defaults = provider.getDefaultParameters(model);

        assertThat(defaults)
                .containsEntry("E_input", 130)
                .containsEntry("h_y_1", 0.8)
                .containsEntry("h_y_2", 0.8)
                .containsEntry("h_x_1", 0.8)
                .containsEntry("h_x_2", 0.1);
    }

    @Test
    void returnsDefaultsForSecondModel() {
        ModelRequest model = new ModelRequest();
        model.setOrder(29);
        model.setLanguage(ru.nsu.fit.evdokimova.supervisor.model.ModelLanguage.C);

        Map<String, Object> defaults = provider.getDefaultParameters(model);

        assertThat(defaults)
                .containsEntry("c_y", 3_950_000_000L)
                .containsEntry("xSampleSize", 600_000L)
                .containsEntry("d_y", 55_000_000L)
                .containsEntry("E_start", 30.0)
                .containsEntry("t", 10L);
    }

    @Test
    void throwsForUnknownModelOrder() {
        ModelRequest model = new ModelRequest();
        model.setOrder(99);
        model.setLanguage(ru.nsu.fit.evdokimova.supervisor.model.ModelLanguage.JAVA);

        assertThatThrownBy(() -> provider.getDefaultParameters(model))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No default parameters defined for model order 99");
    }
}
