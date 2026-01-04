//package ru.nsu.fit.evdokimova.supervisor.service;
//
//import com.github.dockerjava.api.DockerClient;
//import com.github.dockerjava.api.command.BuildImageResultCallback;
//import com.github.dockerjava.api.command.CreateContainerResponse;
//import com.github.dockerjava.api.command.WaitContainerResultCallback;
//import com.github.dockerjava.api.model.Bind;
//import com.github.dockerjava.api.model.HostConfig;
//import com.github.dockerjava.api.model.Volume;
//import lombok.AllArgsConstructor;
//import org.apache.commons.io.FileUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.Set;
//
//
//@AllArgsConstructor
//@Service
//public class DockerService {
//    private static final Logger logger = LoggerFactory.getLogger(DockerService.class);
//    private final DockerClient dockerClient;
//
//    private static final String DOCKERFILE_C_CONTENT = """
//            FROM alpine:latest AS builder
//            RUN apk add --no-cache build-base cmake jansson-dev git
//            WORKDIR /app
//            COPY . .
//            RUN mkdir build && cd build && cmake .. && make
//
//            FROM alpine:latest
//            RUN apk add --no-cache jansson
//            WORKDIR /app
//            COPY --from=builder /app/build/difract .
//            RUN mkdir -p /input /output /json
//            CMD ["sh", "-c", "echo START; ls -l /input; cat /input/start.json; echo RUN; ./difract; echo END"]
//            """;
//
//    private static final String DOCKERFILE_PYTHON_CONTENT = """
//            FROM python:3.11-slim
//            WORKDIR /app
//            COPY test.py .
//            RUN mkdir -p /input /output
//
//            CMD ["python", "test.py"]
//            """;
//
////    CMD ["sh", "-c", "echo START; ls -l /input; cat /input/start.json; echo RUN; ./difract; echo END"]
////    CMD [./difract]
//
//    public void buildAndRunCModel(Path modelSourceDir, Path startJsonPath) throws Exception {
//        logger.info("Building model from: {}", modelSourceDir);
//
//        Path tempDir = Files.createTempDirectory("build-");
//        try {
//            FileUtils.copyDirectory(modelSourceDir.toFile(), tempDir.toFile());
//            Files.writeString(tempDir.resolve("Dockerfile"), DOCKERFILE_C_CONTENT);
//
//            dockerClient.buildImageCmd()
//                    .withDockerfile(tempDir.resolve("Dockerfile").toFile())
//                    .withTags(Set.of("model-2:latest"))
//                    .exec(new BuildImageResultCallback() {
//                        @Override
//                        public void onNext(com.github.dockerjava.api.model.BuildResponseItem item) {
//                            if (item.getStream() != null) {
//                                logger.info("Build: {}", item.getStream().trim());
//                            }
//                        }
//                    })
//                    .awaitCompletion();
//
//            logger.info("Image built: model-2:latest");
//
//            Path endDir = Paths.get("/home/darya/skif_platform/development/supervisor/end_json_files");
//            Path jsonDir = Paths.get("/home/darya/skif_platform/development/supervisor/model_json");
//
//            CreateContainerResponse container = dockerClient.createContainerCmd("model-2:latest")
//                    .withTty(true)
//                    .withAttachStdout(true)
//                    .withAttachStderr(true)
////                    .withCmd("./difract") // это все ломало РРРР
//                    .withHostConfig(HostConfig.newHostConfig()
//                            .withBinds(
//                                    new Bind(startJsonPath.toString(), new Volume("/input/start2.json")),
//                                    new Bind(endDir.toString(), new Volume("/output")),
//                                    new Bind(jsonDir.toString(), new Volume("/json"))
//                            )
//                    )
//                    .exec();
//
//            String containerId = container.getId();
//            logger.info("Starting container: {}", containerId);
//            dockerClient.startContainerCmd(containerId).exec();
//
//            WaitContainerResultCallback waitCallback = new WaitContainerResultCallback();
//            dockerClient.waitContainerCmd(containerId)
//                    .exec(waitCallback)
//                    .awaitCompletion();
//
//            logger.info("!!! Container FINISHED: {}", containerId);
//
//        } catch (Exception e) {
//            logger.error("Error checking host output dir", e);
//        }
//
//        finally {
//            FileUtils.deleteDirectory(tempDir.toFile());
//        }
//    }
//
//    public void buildAndRunPythonModel(Path modelSourceDir, Path startJsonPath) throws Exception {
//        logger.info("Building model from: {}", modelSourceDir);
//
//        Path tempDir = Files.createTempDirectory("build-");
//        try {
//            FileUtils.copyDirectory(modelSourceDir.toFile(), tempDir.toFile());
//            Files.writeString(tempDir.resolve("Dockerfile"), DOCKERFILE_PYTHON_CONTENT);
//
//            dockerClient.buildImageCmd()
//                    .withDockerfile(tempDir.resolve("Dockerfile").toFile())
//                    .withTags(Set.of("model-1:latest"))
//                    .exec(new BuildImageResultCallback() {
//                        @Override
//                        public void onNext(com.github.dockerjava.api.model.BuildResponseItem item) {
//                            if (item.getStream() != null) {
//                                logger.info("Build: {}", item.getStream().trim());
//                            }
//                        }
//                    })
//                    .awaitCompletion();
//
//            logger.info("Image built: model-1:latest");
//
//            Path endDir = Paths.get("/home/darya/skif_platform/development/supervisor/end_json_files");
//            Path jsonDir = Paths.get("/home/darya/skif_platform/development/supervisor/model_json");
//
//            CreateContainerResponse container = dockerClient.createContainerCmd("model-1:latest")
//                    .withTty(true)
//                    .withAttachStdout(true)
//                    .withAttachStderr(true)
////                    .withCmd("./difract") // это все ломало РРРР
//                    .withHostConfig(HostConfig.newHostConfig()
//                            .withBinds(
//                                    new Bind(startJsonPath.toString(), new Volume("/input/start1.json")),
//                                    new Bind(endDir.toString(), new Volume("/output")),
//                                    new Bind(jsonDir.toString(), new Volume("/json"))
//                            )
//                    )
//                    .exec();
//
//            String containerId = container.getId();
//            logger.info("Starting container: {}", containerId);
//            dockerClient.startContainerCmd(containerId).exec();
//
//            WaitContainerResultCallback waitCallback = new WaitContainerResultCallback();
//            dockerClient.waitContainerCmd(containerId)
//                    .exec(waitCallback)
//                    .awaitCompletion();
//
//            logger.info("!!! Container FINISHED: {}", containerId);
//
//        } catch (Exception e) {
//            logger.error("Error checking host output dir", e);
//        }
//
//        finally {
//            FileUtils.deleteDirectory(tempDir.toFile());
//        }
//    }
//}