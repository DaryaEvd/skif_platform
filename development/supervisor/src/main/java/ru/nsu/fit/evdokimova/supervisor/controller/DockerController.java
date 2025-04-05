package ru.nsu.fit.evdokimova.supervisor.controller;

import com.github.dockerjava.api.model.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import ru.nsu.fit.evdokimova.supervisor.service.DockerService;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/docker")
public class DockerController {
    private static final Logger logger = LoggerFactory.getLogger(DockerController.class);
    private final DockerService dockerService;

    public DockerController(DockerService dockerService) {
        this.dockerService = dockerService;
    }

    @GetMapping("/containers")
    public List<Container> getContainers() {
        return dockerService.listContainers();
    }

//    @PostMapping("/start-model")
//    public void startModel(@RequestParam String modelName,
//                           @RequestParam String version,
//                           @RequestParam String startJsonPath,
//                           @RequestParam String containerName) {
//        dockerService.runModelContainer(modelName, version, startJsonPath, containerName);
//    }

//    @PostMapping("/run-step1")
//    public String runStep1Container() {
//        try {
//            Path projectDir = Path.of("/home/darya/RiderProjects/Step1/Step1");
//
//            String imageId = dockerService.buildDotnetImage(projectDir, "step1-app:latest");
//
//            String hostInputPath = "/home/darya/skif_platform/development/supervisor/start_json_files";
//            String hostOutputPath = "/home/darya/skif_platform/development/supervisor/end_json_files";
//
//            String containerId = dockerService.createAndStartContainer("step1-app:latest",
//                    hostInputPath, hostOutputPath);
//
//            return "Container started with ID: " + containerId;
//        } catch (Exception e) {
//            logger.error("Error running container", e);
//            return "Error: " + e.getMessage();
//        }
//    }
}