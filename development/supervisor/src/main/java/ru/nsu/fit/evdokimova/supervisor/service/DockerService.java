package ru.nsu.fit.evdokimova.supervisor.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import lombok.AllArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.nsu.fit.evdokimova.supervisor.model.ModelRequest;
import ru.nsu.fit.evdokimova.supervisor.utils.DockerGeneration.IDockerfileGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static ru.nsu.fit.evdokimova.supervisor.utils.Constants.*;

@AllArgsConstructor
@Service
public class DockerService {
    private static final Logger logger = LoggerFactory.getLogger(ExperimentService.class);
    private final DockerClient dockerClient;
    private final DockerfileGeneratorFactory dockerfileGeneratorFactory;

    public List<Container> listContainers() {
        return dockerClient.listContainersCmd().exec();
    }

    public String buildAndRunModel(ModelRequest model) throws Exception {

        IDockerfileGenerator generator = dockerfileGeneratorFactory.getGenerator(model.getLanguage());
        String dockerfileContent = generator.generateDockerfile();

        Path projectDir = Path.of(model.getModelPath());
        logger.info("path to project dir is {}", projectDir);

        Path tempDir = prepareTempDirectory(projectDir, dockerfileContent);

        try {
        String imageName = buildImage(tempDir, "model-" + model.getOrder());
        logger.info("built image with name {}", imageName);

        return runContainer(imageName, model.getOrder());
        } finally {
            FileUtils.deleteDirectory(tempDir.toFile());
        }
    }

    private Path prepareTempDirectory(Path sourceDir, String dockerfileContent) throws IOException {
        Path tempDir = Files.createTempDirectory("docker-build");
        FileUtils.copyDirectory(sourceDir.toFile(), tempDir.toFile());

        Path dockerfilePath = tempDir.resolve("Dockerfile");
        Files.writeString(dockerfilePath, dockerfileContent);

        return tempDir;
    }

    private String buildImage(Path buildContext, String tag) throws Exception {
        return dockerClient.buildImageCmd()
                .withDockerfile(buildContext.resolve("Dockerfile").toFile())
                .withTags(Set.of(tag))
                .exec(new BuildImageResultCallback())
                .awaitImageId();
    }

    private String runContainer(String imageName, int order) {
        CreateContainerResponse container = dockerClient
            .createContainerCmd(imageName)
            .withHostConfig(HostConfig.newHostConfig())
            .withBinds(
                new Bind(
                    INPUT_PATH_HOST,
                    new Volume(INPUT_PATH_CONTAINER)
                ),
                new Bind(
                    OUTPUT_PATH_HOST,
                    new Volume(OUTPUT_PATH_CONTAINER ))
            )
            .exec();

        logger.info("container is going to start");

        dockerClient.startContainerCmd(container.getId()).exec();
        return container.getId();
    }

    void waitForContainerCompletion(String containerId) {
        try {
            dockerClient.waitContainerCmd(containerId)
                    .exec(new WaitContainerResultCallback())
                    .awaitCompletion(CONTAINER_TIMEOUT.toMinutes(), TimeUnit.MINUTES);

            InspectContainerResponse inspect = dockerClient.inspectContainerCmd(containerId).exec();
            if (inspect.getState().getExitCode() != 0) {
                throw new RuntimeException(String.format(
                        "Container %s failed with exit code %d. Error: %s",
                        containerId,
                        inspect.getState().getExitCode(),
                        inspect.getState().getError()
                ));
            }

            logger.info("Container {} completed successfully", containerId);
        } catch (Exception e) {
            logger.error("Error waiting for container {}", containerId, e);
            throw new RuntimeException("Container execution failed", e);
        }
    }
}