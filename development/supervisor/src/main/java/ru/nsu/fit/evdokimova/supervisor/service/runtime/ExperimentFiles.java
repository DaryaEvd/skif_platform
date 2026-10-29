package ru.nsu.fit.evdokimova.supervisor.service.runtime;

import com.fasterxml.jackson.databind.*;
import org.springframework.stereotype.Component;
import ru.nsu.fit.evdokimova.supervisor.configuration.MainPaths;
import java.io.*;
import java.nio.file.*;
import java.time.Instant;

@Component
public class ExperimentFiles {
    public final ObjectMapper mapper;
    public final Path root;
    public ExperimentFiles(ObjectMapper mapper, MainPaths paths) throws IOException {
        this.mapper = mapper;
        root = paths.getProjectRoot().toAbsolutePath().resolve(paths.getExperimentsRoot()).normalize();
        Files.createDirectories(root);
    }
    public Path experiment(long id) { return root.resolve(Long.toString(id)); }
    public synchronized void write(Path path, Object value) throws IOException { bytes(path, mapper.writeValueAsBytes(value)); }
    public synchronized void bytes(Path path, byte[] bytes) throws IOException {
        Files.createDirectories(path.getParent());
        Path temp = Files.createTempFile(path.getParent(), ".write-", ".tmp");
        try {
            Files.write(temp, bytes);
            Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }
    public synchronized void append(Path path, String text) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, text + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
    public void event(Path experiment, String message) {
        try { append(experiment.resolve("events.log"), Instant.now() + " " + message); }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }
    public JsonNode read(Path path) throws IOException { return mapper.readTree(path.toFile()); }
}
