package ru.nsu.fit.evdokimova.supervisor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;
import ru.nsu.fit.evdokimova.supervisor.configuration.MainPaths;
import ru.nsu.fit.evdokimova.supervisor.model.ModelLanguage;
import ru.nsu.fit.evdokimova.supervisor.model.ModelRequest;
import ru.nsu.fit.evdokimova.supervisor.model.RequestExperimentFromClient;
import ru.nsu.fit.evdokimova.supervisor.service.execmodels.ModelExecutor;
import ru.nsu.fit.evdokimova.supervisor.service.execmodels.ModelExecutorRegistry;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExperimentServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void processExperimentRunsModelsByOrderAndPassesPreviousOutputToNextStartJson() throws Exception {
        StartJsonFactory startJsonFactory = mock(StartJsonFactory.class);
        ObjectMapper objectMapper = new ObjectMapper();
        Path startDir = tempDir.resolve("start");
        Path endDir = tempDir.resolve("end");
        Path interModelDir = tempDir.resolve("inter");
        Files.createDirectories(endDir);
        Files.createDirectories(interModelDir);

        ModelRequest secondInInput = model("second", 2);
        ModelRequest firstInInput = model("first", 1);

        when(startJsonFactory.create(eq(firstInInput), eq(null)))
                .thenReturn(Map.of("start", "first"));
        when(startJsonFactory.create(eq(secondInInput), eq(Map.of("firstResult", 10))))
                .thenReturn(Map.of("start", "second"));
        doAnswer(invocation -> {
            Path startFile = invocation.getArgument(0);
            Files.createDirectories(startFile.getParent());
            Files.writeString(startFile, "{}");
            return startFile;
        }).when(startJsonFactory).writeStartFile(
                any(Path.class),
                org.mockito.ArgumentMatchers.<Map<String, Object>>any()
        );

        RecordingExecutor executor = new RecordingExecutor(objectMapper);
        ExperimentService service = new ExperimentService(
                startJsonFactory,
                new ModelExecutorRegistry(List.of(executor)),
                objectMapper,
                mainPaths(startDir, endDir, interModelDir)
        );

        service.processExperiment(new RequestExperimentFromClient(
                1L,
                "experiment",
                List.of(secondInInput, firstInInput)
        ));

        assertThat(executor.executedModelNames).containsExactly("first", "second");
        assertThat(executor.startFiles).containsExactly(
                startDir.resolve("start1.json"),
                startDir.resolve("start2.json")
        );
        assertThat(executor.endDirs).containsOnly(endDir);
        assertThat(executor.interModelDirs).containsOnly(interModelDir);

        InOrder inOrder = inOrder(startJsonFactory);
        inOrder.verify(startJsonFactory).create(firstInInput, null);
        inOrder.verify(startJsonFactory).writeStartFile(startDir.resolve("start1.json"), Map.of("start", "first"));
        inOrder.verify(startJsonFactory).create(secondInInput, Map.of("firstResult", 10));
        inOrder.verify(startJsonFactory).writeStartFile(startDir.resolve("start2.json"), Map.of("start", "second"));
    }

    private static ModelRequest model(String name, int order) {
        ModelRequest model = new ModelRequest();
        model.setModelId(String.valueOf(order));
        model.setName(name);
        model.setOrder(order);
        model.setLanguage(ModelLanguage.PYTHON);
        model.setModelPath(Path.of("model_" + order));
        return model;
    }

    private static MainPaths mainPaths(Path startDir, Path endDir, Path interModelDir) {
        MainPaths mainPaths = new MainPaths();
        mainPaths.setStartJsonDirPath(startDir);
        mainPaths.setEndJsonDirPath(endDir);
        mainPaths.setInterModelJsonDirPath(interModelDir);
        mainPaths.setModelsRootDirPath(Path.of("models"));
        return mainPaths;
    }

    private static class RecordingExecutor implements ModelExecutor {
        private final ObjectMapper objectMapper;
        private final List<String> executedModelNames = new ArrayList<>();
        private final List<Path> startFiles = new ArrayList<>();
        private final List<Path> endDirs = new ArrayList<>();
        private final List<Path> interModelDirs = new ArrayList<>();

        private RecordingExecutor(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public boolean supports(ModelLanguage language) {
            return language == ModelLanguage.PYTHON;
        }

        @Override
        public Path execute(ModelRequest model, Path startFile, Path endDir, Path modelJsonDir) throws Exception {
            executedModelNames.add(model.getName());
            startFiles.add(startFile);
            endDirs.add(endDir);
            interModelDirs.add(modelJsonDir);

            Path endJson = endDir.resolve("end" + model.getOrder() + ".json");
            Map<String, Object> output = model.getOrder() == 1
                    ? Map.of("firstResult", 10)
                    : Map.of("secondResult", 20);
            objectMapper.writeValue(endJson.toFile(), output);
            return endJson;
        }
    }
}
