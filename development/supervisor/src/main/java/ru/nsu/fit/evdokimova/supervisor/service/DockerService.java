package ru.nsu.fit.evdokimova.supervisor.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@AllArgsConstructor
@Service
public class DockerService {
    private final DockerClient dockerClient;


    public List<Container> listContainers() {
        return dockerClient.listContainersCmd().exec();
    }

//    @PostConstruct
//    public void init() {
//        System.out.println("=== Docker Containers ===");
//        listContainers().forEach(container ->
//                System.out.println(
//                        "ID: " + container.getId() +
//                                " | Image: " + container.getImage() +
//                                " | Status: " + container.getStatus()
//                )
//        );
//    }

//    public void configure() {
//        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
//
//        DockerClient client = DockerClientBuilder.getInstance(config).build();
//
//        List <Container> containers = client.listContainersCmd().exec();
//        containers.forEach(System.out::println);
//    }
}
