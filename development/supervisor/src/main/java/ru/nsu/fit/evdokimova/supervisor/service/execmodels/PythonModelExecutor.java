package ru.nsu.fit.evdokimova.supervisor.service.execmodels;

import com.github.dockerjava.api.DockerClient;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.nsu.fit.evdokimova.supervisor.configuration.MainPaths;
import ru.nsu.fit.evdokimova.supervisor.model.ModelLanguage;
import ru.nsu.fit.evdokimova.supervisor.model.ModelRequest;

import java.nio.file.Path;

@Component
public class PythonModelExecutor
        extends AbstractDockerModelExecutor {

    public PythonModelExecutor(
            DockerClient dockerClient,
            MainPaths mainPaths
    ) {
        super(dockerClient, mainPaths,
                LoggerFactory.getLogger(PythonModelExecutor.class));
    }

    @Override
    public boolean supports(ModelLanguage language) {
        return language == ModelLanguage.PYTHON;
    }

    @Override
    protected String createImageName(ModelRequest model) {
        return "model-python-" + model.getModelId();
    }

    @Override
    protected String createDockerfileContent(ModelRequest model) {
        return """
            FROM python:3.11-slim
            WORKDIR /app
            COPY . .
            RUN mkdir -p /input /output /json
            CMD ["python", "test.py"]
            """;
    }
}
