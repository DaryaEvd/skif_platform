package ru.nsu.fit.evdokimova.supervisor.service;

import org.springframework.stereotype.Component;
import ru.nsu.fit.evdokimova.supervisor.model.ModelRequest;

import java.util.HashMap;
import java.util.Map;

@Component
public class HardcodedModelProvider implements IModelDefaultsProvider{
    @Override
    public Map<String, Object> getDefaultParameters(ModelRequest model) {

        Map<String, Object> params = new HashMap<>();

        switch (model.getOrder()) {
            case 1 ->  {
                params.put("E_input", 130);
                params.put("h_y_1", 0.8);
                params.put("h_y_2", 0.8);
                params.put("h_x_1", 0.8);
                params.put("h_x_2", 0.1);
            }

            case 2 -> {
                params.put("c_x", 0L);
                params.put("c_y", 3950000000L);
                params.put("c_z", 0L);
                params.put("s_x", 0L);
                params.put("s_y", 0L);
                params.put("s_z", 0L);
                params.put("omega", 0.0);
                params.put("kappa", 0.0);
                params.put("phi", 0.0);
                params.put("xSampleSize", 600000L);
                params.put("ySampleSize", 600000L);
                params.put("zSampleSize", 600000L);
                params.put("d_x", 0L);
                params.put("d_y", 55000000L);
                params.put("d_z", 0L);
                params.put("theta", 0.0);
                params.put("beta", 0.0);
                params.put("gammaValue", 0.0);
                params.put("sU", 50000L);
                params.put("sB", 50000L);
                params.put("sR", 50000L);
                params.put("sL", 50000L);
                params.put("E_start", 30.0);
                params.put("E_end", 30.0);
                params.put("t", 10L);
            }


            default -> throw new IllegalArgumentException(
                    "No default parameters defined for model order " + model.getOrder()
            );
        }

        return params;
    }
}
