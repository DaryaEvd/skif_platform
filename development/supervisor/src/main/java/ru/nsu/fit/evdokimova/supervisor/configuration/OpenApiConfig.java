package ru.nsu.fit.evdokimova.supervisor.configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
@Configuration
public class OpenApiConfig {
    @Bean OpenApiCustomizer experimentExample(ObjectMapper mapper) {
        return api -> {
            try {
                Object example=mapper.readValue(new ClassPathResource("examples/python-c.json").getInputStream(),Object.class);
                api.getPaths().get("/api/experiments/start").getPost().getRequestBody()
                    .getContent().get("application/json").setExample(example);
            } catch(Exception e) {throw new IllegalStateException("Cannot load experiment OpenAPI example",e);}
        };
    }
}
