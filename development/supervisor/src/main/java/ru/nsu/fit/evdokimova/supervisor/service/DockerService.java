package ru.nsu.fit.evdokimova.supervisor.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;

@AllArgsConstructor
@Service
public class DockerService {
    private static final Logger logger = LoggerFactory.getLogger(ExperimentService.class);

    private final DockerClient dockerClient;

    public List<Container> listContainers() {
        return dockerClient.listContainersCmd().exec();
    }

    public void runModelContainer(String modelPath, String modelVersion, String startJsonPath, String containerName) {
        try {
            if (modelVersion == null || modelVersion.trim().isEmpty()) {
                modelVersion = "latest";
            }

            Path tempDir = Files.createTempDirectory("docker_build_");
            logger.info("Created temp directory: {}", tempDir);

            copyModelFiles(Paths.get(modelPath), tempDir);

            Path dataDir = tempDir.resolve("data");
            Files.createDirectories(dataDir);
            Files.copy(Paths.get(startJsonPath), dataDir.resolve("start.json"));

            Path dockerfilePath = tempDir.resolve("Dockerfile");
            String dockerfileContent = """
                            FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
                            WORKDIR /src
                            COPY . .
                            RUN dotnet publish -c Release -o /app/publish

                            FROM mcr.microsoft.com/dotnet/runtime:8.0 AS runtime
                            WORKDIR /app
                            COPY --from=build /app/publish .
                            COPY data/start.json /app/data/start.json
                            ENTRYPOINT ["dotnet", "Step1.dll"]
                    """;
            Files.write(dockerfilePath, dockerfileContent.getBytes());
            logger.info("Dockerfile created at: {}", dockerfilePath);

            String imageName = containerName + ":" + modelVersion;
            String imageId = dockerClient.buildImageCmd(tempDir.toFile())
                    .withTags(Set.of(imageName))
                    .exec(new BuildImageResultCallback())
                    .awaitImageId();

            logger.info("Docker image built: {}", imageId);

            CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                    .withName(containerName)
                    .exec();

            dockerClient.startContainerCmd(container.getId()).exec();
            logger.info("Container '{}' started", containerName);

            waitForContainerToStop(container.getId());

            String endJsonPath = startJsonPath.replace("start", "end");
            Path pathToEndJson = Paths.get(endJsonPath);
            if (Files.exists(pathToEndJson)) {
                logger.info("Model '{}' finished, result: {}", modelPath, new String(Files.readAllBytes(pathToEndJson)));
            } else {
                logger.warn("End file '{}' not found", endJsonPath);
            }

        } catch (Exception e) {
            logger.error("Error running container", e);
        }
    }

    private void copyModelFiles(Path source, Path destination) throws IOException {
        Files.walk(source)
                .filter(path -> !path.toString().matches(".*(\\.git|bin|obj|\\.idea|\\.vs).*"))
                .forEach(file -> {
                    try {
                        Path destFile = destination.resolve(source.relativize(file));
                        if (Files.isDirectory(file)) {
                            Files.createDirectories(destFile);
                        } else {
                            Files.copy(file, destFile, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        logger.error("Error copying file {}", file, e);
                    }
                });
    }

    private void waitForContainerToStop(String containerId) throws InterruptedException {
        while (true) {
            InspectContainerResponse inspectResponse = dockerClient.inspectContainerCmd(containerId).exec();
            if (Boolean.FALSE.equals(inspectResponse.getState().getRunning())) {
                logger.info("Container '{}' stopped", containerId);
                break;
            }
            Thread.sleep(1000);
        }
    }


    //todo: maybe delete then? or decompose runModelContainer
    public void runExperiment(List<Map<String, Object>> models) {
        for (Map<String, Object> model : models) {
            String name = (String) model.get("name");
            String version = (String) model.get("version");
            Integer order = (Integer) model.get("order");
            String startJsonPath = "/home/darya/skif_platform/development/supervisor/start_json_files/start" + order + ".json";

            runModelContainer(name, version, startJsonPath, "model-" + order);
        }
    }
}