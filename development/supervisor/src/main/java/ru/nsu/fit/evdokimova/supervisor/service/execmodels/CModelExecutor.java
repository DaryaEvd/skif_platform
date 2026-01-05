package ru.nsu.fit.evdokimova.supervisor.service.execmodels;

import com.github.dockerjava.api.DockerClient;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.nsu.fit.evdokimova.supervisor.model.ModelLanguage;
import ru.nsu.fit.evdokimova.supervisor.model.ModelRequest;

import java.nio.file.Path;

@Component
public class CModelExecutor extends AbstractDockerModelExecutor {

    public CModelExecutor(
            DockerClient dockerClient,
            @Value("${supervisor.models.root}") Path modelsRoot
    ) {
        super(dockerClient, modelsRoot,
                LoggerFactory.getLogger(CModelExecutor.class));
    }

    @Override
    public boolean supports(ModelLanguage language) {
        return language == ModelLanguage.C;
    }

    @Override
    protected String createImageName(ModelRequest model) {
        return "model-c-" + model.getModelId();
    }

    @Override
    protected String createDockerfileContent(ModelRequest model) {
        return """
            FROM alpine:latest AS builder
            RUN apk add --no-cache build-base cmake jansson-dev git
            WORKDIR /app
            COPY . .
            RUN mkdir build && cd build && cmake .. && make
            
            FROM alpine:latest
            RUN apk add --no-cache jansson
            WORKDIR /app
            COPY --from=builder /app/build/difract .
            RUN mkdir -p /input /output /json
            CMD ["sh", "-c", "echo START; ls -l /input; cat /input/start.json; echo RUN; ./difract; echo END"]
            """;
    }
}
