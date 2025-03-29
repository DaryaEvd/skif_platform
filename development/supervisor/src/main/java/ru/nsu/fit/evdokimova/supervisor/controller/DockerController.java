package ru.nsu.fit.evdokimova.supervisor.controller;

import com.github.dockerjava.api.model.Container;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
}