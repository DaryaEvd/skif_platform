package ru.nsu.fit.evdokimova.supervisor.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import lombok.AllArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.nsu.fit.evdokimova.supervisor.model.ModelRequest;
import ru.nsu.fit.evdokimova.supervisor.utils.DockerGeneration.IDockerfileGenerator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

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

        String imageName = buildImage(tempDir, model.getName().toLowerCase());

        return runContainer(imageName);
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

    private String runContainer(String imageName) {
        CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                .withHostConfig(HostConfig.newHostConfig())
                .withBinds(
                        new Bind(INPUT_PATH_HOST, new Volume(INPUT_PATH_CONTAINER)),
                        new Bind(OUTPUT_PATH_HOST, new Volume(OUTPUT_PATH_CONTAINER))
                )
                .exec();

        dockerClient.startContainerCmd(container.getId()).exec();
        return container.getId();
    }

}