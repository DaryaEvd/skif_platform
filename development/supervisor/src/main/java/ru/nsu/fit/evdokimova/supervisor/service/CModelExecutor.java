package ru.nsu.fit.evdokimova.supervisor.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.nsu.fit.evdokimova.supervisor.model.ModelLanguage;
import ru.nsu.fit.evdokimova.supervisor.model.ModelRequest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

@Component
public class CModelExecutor implements ModelExecutor {

    private static final Logger log = LoggerFactory.getLogger(CModelExecutor.class);

    private final DockerClient dockerClient;

    @Value("${supervisor.models.root}")
    private Path modelsRoot;

    public CModelExecutor(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    @Override
    public boolean supports(ModelLanguage language) {
        return language == ModelLanguage.C;
    }

    @Override
    public Path execute(ModelRequest model,
                        Path startFile,
                        Path endDir,
                        Path modelJsonDir) throws Exception {
        Path modelSourceDir = modelsRoot.resolve(model.getModelPath()).normalize();


        Path tempDir = Files.createTempDirectory("c-model-");
        Path outputDir = Files.createTempDirectory("c-output-");

        try {

//            Path modelSourceDir = modelsRoot.resolve(model.getModelPath());
            FileUtils.copyDirectory(modelSourceDir.toFile(), tempDir.toFile());

            if (!Files.isDirectory(modelSourceDir)) {
                throw new IllegalArgumentException(
                        "Model directory does not exist: " + modelSourceDir
                );
            }

            FileUtils.copyDirectory(model.getModelPath().toFile(), tempDir.toFile());

            Files.writeString(
                    tempDir.resolve("Dockerfile"),
                    """
                    FROM alpine:latest AS builder
                    RUN apk add --no-cache build-base cmake jansson-dev
                    WORKDIR /app
                    COPY . .
                    RUN mkdir build && cd build && cmake .. && make

                    FROM alpine:latest
                    RUN apk add --no-cache jansson
                    WORKDIR /app
                    COPY --from=builder /app/build/difract .
                    RUN mkdir -p /input /output
                    CMD ["./difract"]
                    """
            );

            String imageName = "model-c-" + model.getModelId();

            dockerClient.buildImageCmd()
                    .withDockerfile(tempDir.resolve("Dockerfile").toFile())
                    .withTags(Set.of(imageName))
                    .exec(new BuildImageResultCallback())
                    .awaitCompletion();

            CreateContainerResponse container =
                    dockerClient.createContainerCmd(imageName)
                            .withHostConfig(
                                    HostConfig.newHostConfig().withBinds(
                                            new Bind(
                                                    startFile.toAbsolutePath().toString(),
                                                    new Volume("/input/start.json")
                                            ),
                                            new Bind(
                                                    endDir.toAbsolutePath().toString(),
                                                    new Volume("/output")
                                            ),
                                            new Bind(
                                                    modelJsonDir.toAbsolutePath().toString(),
                                                    new Volume("/json")
                                            )
                                    )
                            )
                            .exec();

            dockerClient.startContainerCmd(container.getId()).exec();
            dockerClient.waitContainerCmd(container.getId())
                    .exec(new WaitContainerResultCallback())
                    .awaitCompletion();

            Path endJson = outputDir.resolve("end.json");
            if (!Files.exists(endJson)) {
                throw new IllegalStateException("end.json not produced by C model");
            }

            return endJson;

        } finally {
            FileUtils.deleteDirectory(tempDir.toFile());
        }
    }
}

