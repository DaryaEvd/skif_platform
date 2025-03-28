package ru.nsu.fit.evdokimova.supervisor.controller;


import com.github.dockerjava.api.model.Container;
import org.springframework.web.bind.annotation.*;
import ru.nsu.fit.evdokimova.supervisor.service.DockerService;

import java.net.UnknownHostException;
import java.util.List;

@RestController
@RequestMapping("/api/docker")
public class DockerController {

//    private final DockerService dockerService;
//
//
//    public DockerController(DockerService dockerService) {
//        this.dockerService = dockerService;
//    }
//
//    @GetMapping("")
//    public List<Container> listContainers(@RequestParam(required = false, defaultValue = "true") boolean showAll) {
//        return dockerService.listContainers(showAll);
//    }
//
//    @PostMapping("/{id}/start")
//    public void startContainer(@PathVariable String id) {
//        dockerService.startContainer(id);
//    }
//
//    @PostMapping("/{id}/stop")
//    public void stopContainer(@PathVariable String id) {
//        dockerService.stopContainer(id);
//    }
//
//    @DeleteMapping("/{id}")
//    public void removeContainer(@PathVariable String id) {
//        dockerService.removeContainer(id);
//    }
//
//    @PostMapping("")
//    public void createContainer(@RequestParam String imageName) {
//        dockerService.createContainer(imageName);
//    }


    private final DockerService dockerService;

    public DockerController(DockerService dockerService) {
        this.dockerService = dockerService;
    }

    @GetMapping("/containers")
    public List<Container> getContainers()  {
        return dockerService.listContainers();
    }
}
