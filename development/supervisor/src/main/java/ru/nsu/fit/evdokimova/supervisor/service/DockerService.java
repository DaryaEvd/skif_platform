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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

@AllArgsConstructor
@Service
public class DockerService {
    private static final Logger logger = LoggerFactory.getLogger(ExperimentService.class);
    private final DockerClient dockerClient;

    public List<Container> listContainers() {
        return dockerClient.listContainersCmd().exec();
    }

    public String buildImage(String dockerfilePath, String tag) throws InterruptedException {
        return dockerClient.buildImageCmd()
                .withDockerfile(new File(dockerfilePath))
                .withTags(new HashSet<>(Collections.singleton(tag)))
                .exec(new BuildImageResultCallback())
                .awaitImageId();
    }

    public String createAndStartContainer(String imageName,
                                          String hostInputPath,
                                          String hostOutputPath) {
        String containerInputPath = "/app/input";
        String containerOutputPath = "/app/output";

        CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                .withHostConfig(HostConfig.newHostConfig())
                        .withBinds(
                                new Bind(hostInputPath, new Volume(containerInputPath)),
                                new Bind(hostOutputPath, new Volume(containerOutputPath))
                        )
                        .exec();

        dockerClient.startContainerCmd(container.getId()).exec();

        return container.getId();
    }

    private void validateProjectStructure(Path projectDir) {
        if (!Files.exists(projectDir.resolve("Step1.csproj"))) {
            throw new IllegalArgumentException("Step1.csproj not found in project directory");
        }
        if (!Files.exists(projectDir.resolve("Step1.cs"))) {
            throw new IllegalArgumentException("Program.cs not found in project directory");
        }
    }

    private Path createTempProjectStructure(String dockerfileContent, Path sourceProjectDir) throws IOException {
        Path tempDir = Files.createTempDirectory("docker-build");

        Files.walk(sourceProjectDir)
                .filter(source -> !source.equals(sourceProjectDir))
                .forEach(source -> {
                    Path relativePath = sourceProjectDir.relativize(source);
                    Path dest = tempDir.resolve(relativePath);

                    try {
                        if (!Files.exists(dest.getParent())) {
                            Files.createDirectories(dest.getParent());
                        }

                        if (Files.isDirectory(source)) {
                            if (!Files.exists(dest)) {
                                Files.createDirectory(dest);
                            }
                        } else {
                            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
                        }

                        System.out.println("Copied: " + source + " to " + dest);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to copy file: " + source, e);
                    }
                });

        Path dockerfilePath = tempDir.resolve("Dockerfile");
        Files.writeString(dockerfilePath, dockerfileContent, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);

        return tempDir;
    }

    public String buildDotnetImage(Path projectDir, String tag) throws Exception {

        validateProjectStructure(projectDir);

        if (!Files.isDirectory(projectDir)) {
            throw new IllegalArgumentException("Project directory not found: " + projectDir);
        }

        System.out.println("Building image from: " + projectDir);
        System.out.println("Files in project directory:");
        Files.list(projectDir).forEach(System.out::println);

        String dockerfileContent = """
        FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
        WORKDIR /src
        COPY . .
        RUN dotnet publish "Step1.csproj" -c Release -o /app/publish

        FROM mcr.microsoft.com/dotnet/runtime:8.0 AS runtime
        WORKDIR /app
        COPY --from=build /app/publish .

        RUN mkdir -p /app/input && \\
            mkdir -p /app/output && \\
            chmod -R 777 /app/input && \\
            chmod -R 777 /app/output

        ENTRYPOINT ["dotnet", "Step1.dll"]
        """;

        try {
            Path tempDir = createTempProjectStructure(dockerfileContent, projectDir);

            System.out.println("Temporary build directory content:");
            Files.walk(tempDir).forEach(System.out::println);

            return dockerClient.buildImageCmd()
                    .withDockerfile(tempDir.resolve("Dockerfile").toFile())
                    .withBaseDirectory(tempDir.toFile())
                    .withTags(Set.of(tag))
                    .exec(new BuildImageResultCallback() {
                        @Override
                        public void onNext(BuildResponseItem item) {
                            if (item.getStream() != null) {
                                System.out.print(item.getStream());
                            }
                            if (item.getError() != null) {
                                System.err.print(item.getError());
                            }
                            super.onNext(item);
                        }
                    })
                    .awaitImageId();
        } catch (Exception e) {
            System.err.println("Build failed: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }


}

/*
    private static final Logger logger = LoggerFactory.getLogger(ExperimentService.class);

    private final DockerClient dockerClient;

    public List<Container> listContainers() {
        return dockerClient.listContainersCmd().exec();
    }
    public void runModelContainer(String modelPath, String modelVersion,
                                  String startJsonPath, String containerName) {
        try {
            modelVersion = resolveModelVersion(modelVersion);
            Path tempDir = prepareTempDirectory();
            setupDockerEnvironment(tempDir, modelPath, startJsonPath);
            String imageName = buildDockerImage(tempDir, containerName, modelVersion);
            String containerId = createAndStartContainer(imageName, containerName, startJsonPath);
            processContainerResults(containerId, startJsonPath, modelPath);
        } catch (Exception e) {
            logger.error("Error running container", e);
        }
    }

    private String resolveModelVersion(String modelVersion) {
        return (modelVersion == null || modelVersion.trim().isEmpty())
                ? "latest"
                : modelVersion;
    }

    private Path prepareTempDirectory() throws IOException {
        Path tempDir = Files.createTempDirectory("docker_build_");
        logger.info("Created temp directory: {}", tempDir);
        return tempDir;
    }

    private void setupDockerEnvironment(Path tempDir, String modelPath,
                                        String startJsonPath) throws IOException {
        copyModelFiles(Paths.get(modelPath), tempDir);
        prepareDataDirectory(tempDir, startJsonPath);
        createDockerfile(tempDir);
    }

    private void prepareDataDirectory(Path tempDir, String startJsonPath) throws IOException {
        Path dataDir = tempDir.resolve("data");
        Files.createDirectories(dataDir);
        Files.copy(Paths.get(startJsonPath), dataDir.resolve("start.json"));
    }

    private void createDockerfile(Path tempDir) throws IOException {
        Path dockerfilePath = tempDir.resolve("Dockerfile");
        String dockerfileContent = """
        FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
        WORKDIR /src
        COPY . .
        RUN dotnet publish -c Release -o /app/publish

        FROM mcr.microsoft.com/dotnet/runtime:8.0 AS runtime
        WORKDIR /app
        COPY --from=build /app/publish .
        ENTRYPOINT ["dotnet", "Step1.dll"]
        """;
        Files.write(dockerfilePath, dockerfileContent.getBytes());
        logger.info("Dockerfile created at: {}", dockerfilePath);
    }

    private String buildDockerImage(Path tempDir, String containerName,
                                    String modelVersion) throws DockerException, InterruptedException {
        String imageName = containerName + ":" + modelVersion;
        return dockerClient.buildImageCmd(tempDir.toFile())
                .withTags(Set.of(imageName))
                .exec(new BuildImageResultCallback())
                .awaitImageId();
    }

    private String createAndStartContainer(String imageName, String containerName, String startJsonPath) {
//        CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
//                .withName(containerName)
//                .exec();
//
//        dockerClient.startContainerCmd(container.getId()).exec();
//        logger.info("Container '{}' started", containerName);
//
//        return container.getId();

        File startJsonFile = new File(startJsonPath);

        if (!startJsonFile.exists()) {
            throw new RuntimeException("Start JSON file does not exist: " + startJsonPath);
        }

        HostConfig hostConfig = new HostConfig()
//                .withBinds(new Bind(startJsonPath, new Volume("/app/data/start.json"), AccessMode.ro));
                .withBinds(new Bind("/home/darya/skif_platform/development/supervisor/start_json_files", new Volume("/app/data"), AccessMode.rw));

        CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                .withName(containerName)
                .withHostConfig(hostConfig)
                .exec();

        dockerClient.startContainerCmd(container.getId()).exec();
        logger.info("Container '{}' started, mounted '{}' as '/app/data/start.json'",
                containerName, startJsonPath);

        return container.getId();
    }

    private void processContainerResults(String containerId,
                                         String startJsonPath, String modelPath)
            throws IOException, InterruptedException {
//        waitForContainerToStop(containerId);

//        String containerEndJsonPath = "/app/data/end.json";
        String containerEndJsonPath = "/app/data/end1.json";

        String hostEndJsonPath = startJsonPath.replace("start", "end");
        Path pathToEndJson = Paths.get(hostEndJsonPath);

        waitForEndJson(containerId, containerEndJsonPath, hostEndJsonPath);

         try (InputStream inputStream = dockerClient.copyArchiveFromContainerCmd(containerId, containerEndJsonPath).exec()) {
            Files.copy(inputStream, pathToEndJson, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Copied end.json from container '{}' to '{}'", containerId, hostEndJsonPath);
        } catch (Exception e) {
            logger.warn("Failed to copy end.json from container '{}': {}", containerId, e.getMessage());
        }

        if (Files.exists(pathToEndJson)) {
            logger.info("Model '{}' finished, result: {}",
                    modelPath, new String(Files.readAllBytes(pathToEndJson)));
        } else {
            logger.warn("End file '{}' not found", hostEndJsonPath);
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

    private void waitForEndJson(String containerId, String containerEndJsonPath, String hostEndJsonPath)
            throws IOException, InterruptedException {
        Path parentDir = Paths.get(hostEndJsonPath).getParent();
        String fileName = Paths.get(hostEndJsonPath).getFileName().toString();

        logger.info("Waiting for '{}' to appear in '{}'", fileName, parentDir);

        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            parentDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

            while (true) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                        Path createdFile = (Path) event.context();
                        if (createdFile.getFileName().toString().equals(fileName)) {
                            logger.info("'{}' detected, stopping container '{}'", fileName, containerId);
                            return;
                        }
                    }
                }
                key.reset();
            }
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
     */