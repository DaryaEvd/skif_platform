package ru.nsu.fit.evdokimova.supervisor.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import lombok.AllArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.nsu.fit.evdokimova.supervisor.model.ModelLanguage;
import ru.nsu.fit.evdokimova.supervisor.model.ModelRequest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;

@Component
public class PythonModelExecutor implements ModelExecutor {
    private static final Logger log = LoggerFactory.getLogger(PythonModelExecutor.class);

    private final DockerClient dockerClient;

    public PythonModelExecutor(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    @Value("${supervisor.models.root}")
    private Path modelsRoot;

    @Override
    public boolean supports(ModelLanguage language) {
        return language == ModelLanguage.PYTHON;
    }

    @Override
    public Path execute(
            ModelRequest model,
            Path startFile,
            Path endDir,
            Path modelJsonDir
    ) throws Exception {

        Path modelSourceDir = modelsRoot.resolve(model.getModelPath()).normalize();
        log.info("Using model source dir: {}", modelSourceDir);

        if (!Files.isDirectory(modelSourceDir)) {
            throw new IllegalArgumentException(
                    "Model directory does not exist: " + modelSourceDir
            );
        }

//        Files.createDirectories(endDir);
//        Files.createDirectories(modelJsonDir);

        Path tempDir = Files.createTempDirectory("python-model-");

        try {
            FileUtils.copyDirectory(modelSourceDir.toFile(), tempDir.toFile());

            Files.writeString(
                    tempDir.resolve("Dockerfile"),
                    """
                    FROM python:3.11-slim
                    WORKDIR /app
                    COPY . .
                    RUN mkdir -p /input /output /json
                    CMD ["python", "test.py"]
                    """
            );

            String imageName = "model-python-" + model.getModelId();

            dockerClient.buildImageCmd()
                    .withDockerfile(tempDir.resolve("Dockerfile").toFile())
                    .withTags(Set.of(imageName))
                    .exec(new BuildImageResultCallback() {
                    @Override
                        public void onNext(com.github.dockerjava.api.model.BuildResponseItem item) {
                            if (item.getStream() != null) {
                                log.info("Build: {}", item.getStream().trim());
                            }
                        }
                    })
                    .awaitCompletion();
            log.info("Image built: {}", imageName);

            CreateContainerResponse container =
                    dockerClient.createContainerCmd(imageName)
                            .withTty(true)
                            .withAttachStdout(true)
                            .withAttachStderr(true)
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

            String containerId = container.getId();
            log.info("Starting container: {}", containerId);
            dockerClient.startContainerCmd(containerId).exec();

            WaitContainerResultCallback waitCallback = new WaitContainerResultCallback();
            dockerClient.waitContainerCmd(containerId)
                    .exec(waitCallback)
                    .awaitCompletion();

            log.info("!!! Container FINISHED: {}", containerId);

            Path producedByContainerEndJson = endDir.resolve("end.json");
            log.info("Path for end.json file from container: {} ", producedByContainerEndJson.toAbsolutePath());
            if (!Files.exists(producedByContainerEndJson)) {
                throw new IllegalStateException(
                        "end.json not produced by python model"
                );
            }
            Path finalEndJson = endDir.resolve("end" + model.getOrder() + ".json");

            Files.move(
                    producedByContainerEndJson,
                    finalEndJson,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return finalEndJson;

        } finally {
            FileUtils.deleteDirectory(tempDir.toFile());
        }
    }
}
