package ru.nsu.fit.evdokimova.supervisor.controller;

import com.github.dockerjava.api.model.Container;
import org.springframework.web.bind.annotation.*;
import ru.nsu.fit.evdokimova.supervisor.service.DockerService;

import java.util.List;

@RestController
@RequestMapping("/api/docker")
public class DockerController {
    private final DockerService dockerService;

    public DockerController(DockerService dockerService) {
        this.dockerService = dockerService;
    }

    @GetMapping("/containers")
    public List<Container> getContainers() {
        return dockerService.listContainers();
    }

    @PostMapping("/start-model")
    public void startModel(@RequestParam String modelName,
                           @RequestParam String version,
                           @RequestParam String startJsonPath,
                           @RequestParam String containerName) {
        dockerService.runModelContainer(modelName, version, startJsonPath, containerName);
    }
}