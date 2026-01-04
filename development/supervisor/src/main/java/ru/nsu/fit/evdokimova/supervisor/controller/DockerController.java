package ru.nsu.fit.evdokimova.supervisor.controller;

//import com.github.dockerjava.api.model.Container;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.web.bind.annotation.*;
//import ru.nsu.fit.evdokimova.supervisor.service.DockerService;
//
//import java.nio.file.Path;
//import java.util.List;

//@RestController
//@RequestMapping("/api/docker")
//public class DockerController {
//    private static final Logger logger = LoggerFactory.getLogger(DockerController.class);
//    private final DockerService dockerService;
//
//    public DockerController(DockerService dockerService) {
//        this.dockerService = dockerService;
//    }

//    @GetMapping("/containers")
//    public List<Container> getContainers() {
//        return dockerService.listContainers();
//    }
//}