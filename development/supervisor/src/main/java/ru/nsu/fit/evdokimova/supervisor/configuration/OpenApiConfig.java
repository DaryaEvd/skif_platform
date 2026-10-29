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
                var media=api.getPaths().get("/api/experiments/start").getPost().getRequestBody().getContent().get("application/json");
                media.setExample(null);
                media.addExamples("Python → C",new io.swagger.v3.oas.models.examples.Example().value(example));
                Object unity=mapper.readValue(new ClassPathResource("examples/unity-phi.json").getInputStream(),Object.class);
                media.addExamples("Unity: относительный Phi",new io.swagger.v3.oas.models.examples.Example().value(unity));
            } catch(Exception e) {throw new IllegalStateException("Cannot load experiment OpenAPI example",e);}
        };
    }
}
