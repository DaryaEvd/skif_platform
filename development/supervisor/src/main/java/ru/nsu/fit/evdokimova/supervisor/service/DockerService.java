package ru.nsu.fit.evdokimova.supervisor.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@AllArgsConstructor
@Service
public class DockerService {
    private final DockerClient dockerClient;

//
//    public List<Container> listContainers(boolean all) {
//        return dockerClient.listContainersCmd().withShowAll(all).exec();
//    }
//
//    public List<Image> listImages() {
//        return dockerClient.listImagesCmd().exec();
//    }
//
//    public List<Image> filterImages(String filterName) {
//        return dockerClient.listImagesCmd().withImageNameFilter(filterName).exec();
//    }
//
//    public void startContainer(String containerId) {
//        dockerClient.startContainerCmd(containerId).exec();
//    }
//
//    public void stopContainer(String containerId) {
//        dockerClient.stopContainerCmd(containerId).exec();
//    }
//
//    public void removeContainer(String containerId) {
//        dockerClient.removeContainerCmd(containerId).exec();
//    }
//
//    public void createContainer(String imageName) {
//        dockerClient.createContainerCmd(imageName).exec();
//    }

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
