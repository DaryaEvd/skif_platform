package ru.nsu.fit.evdokimova.supervisor.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@ConfigurationProperties(prefix = "supervisor.paths")
@Getter
@Setter
public class MainPaths {
    private Path projectRoot = Path.of(".");
    private Path experimentsRoot = Path.of("experiment-data");
    private Path modelsRootDirPath;
    private Path startJsonDirPath;
    private Path endJsonDirPath;
    private Path interModelJsonDirPath;
}
