package ru.nsu.fit.evdokimova.supervisor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ru.nsu.fit.evdokimova.supervisor.configuration.MainPaths;

@EnableConfigurationProperties(MainPaths.class)
@SpringBootApplication
public class SupervisorApplication {

	public static void main(String[] args) {
		SpringApplication.run(SupervisorApplication.class, args);
	}
}
