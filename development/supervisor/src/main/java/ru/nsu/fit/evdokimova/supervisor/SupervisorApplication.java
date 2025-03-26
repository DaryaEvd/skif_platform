package ru.nsu.fit.evdokimova.supervisor;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;

@SpringBootApplication
public class SupervisorApplication {

	public static void main(String[] args) {
		SpringApplication.run(SupervisorApplication.class, args);
	}

}
