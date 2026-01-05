package ru.nsu.fit.evdokimova.supervisor.service.execmodels;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import ru.nsu.fit.evdokimova.supervisor.configuration.MainPaths;
import ru.nsu.fit.evdokimova.supervisor.model.ModelRequest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;


public abstract class AbstractDockerModelExecutor
        implements ModelExecutor {

    protected final DockerClient dockerClient;
    protected final MainPaths mainPaths;
    protected final Logger log;

    protected AbstractDockerModelExecutor(
            DockerClient dockerClient,
            MainPaths mainPaths,
            Logger log
    ) {
        this.dockerClient = dockerClient;
        this.mainPaths = mainPaths;
        this.log = log;
    }

    protected abstract String createImageName(ModelRequest model);

    protected abstract String createDockerfileContent(ModelRequest model);

    @Override
    public Path execute(
            ModelRequest model,
            Path startFile,
            Path endDir,
            Path modelJsonDir
    ) throws Exception {

        Path modelSourceDir = mainPaths.getModelsRootDirPath().resolve(model.getModelPath()).normalize();
        log.info("Using model source dir: {}", modelSourceDir);

        if (!Files.isDirectory(modelSourceDir)) {
            throw new IllegalArgumentException(
                    "Model directory does not exist: " + modelSourceDir
            );
        }

        Path tempDir = Files.createTempDirectory(
                model.getLanguage().toString().toLowerCase() + "model-");

        try {
            FileUtils.copyDirectory(modelSourceDir.toFile(), tempDir.toFile());

            Files.writeString(
                    tempDir.resolve("Dockerfile"),
                    createDockerfileContent(model)
            );

            String imageName = createImageName(model);

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
                                            bind(startFile, "/input/start.json"),
                                            bind(endDir, "/output"),
                                            bind(modelJsonDir, "/json")
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

            afterContainerFinished(container.getId());


            Path producedByContainerEndJson = endDir.resolve("end.json");
            log.info("Path for end.json file from container: {} ", producedByContainerEndJson.toAbsolutePath());
            if (!Files.exists(producedByContainerEndJson)) {
                throw new IllegalStateException(
                        "end.json not produced for model " + model.getName() + "with ID:" + model.getModelId()
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

    private Bind bind(Path host, String pathInContainer) {
        return new Bind(
                host.toAbsolutePath().toString(),
                new Volume(pathInContainer)
        );
    }

    protected void afterContainerFinished(String containerId) throws Exception {
        dockerClient.logContainerCmd(containerId)
                .withStdOut(true)
                .withStdErr(true)
                .exec(new ResultCallback.Adapter<>() {
                    @Override
                    public void onNext(Frame item) {
                        log.info("id: {} -   CONTAINER LOG: {}", containerId,
                                new String(item.getPayload()));
                    }
                })
                .awaitCompletion();
        log.info("!!! Container FINISHED: {}", containerId);
    }

}
