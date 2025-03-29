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
}