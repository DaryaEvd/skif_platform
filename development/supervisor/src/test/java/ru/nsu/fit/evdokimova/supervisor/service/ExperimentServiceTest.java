package ru.nsu.fit.evdokimova.supervisor.service;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import ru.nsu.fit.evdokimova.supervisor.configuration.MainPaths;
import ru.nsu.fit.evdokimova.supervisor.model.*;
import ru.nsu.fit.evdokimova.supervisor.service.createparams.HardcodedModelProvider;
import ru.nsu.fit.evdokimova.supervisor.service.execmodels.*;
import ru.nsu.fit.evdokimova.supervisor.service.runtime.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.assertj.core.api.Assertions.*;

class ExperimentServiceTest {
    @TempDir Path root;
    ExperimentService service;
    ObjectMapper mapper=new ObjectMapper().registerModule(new JavaTimeModule());
    @AfterEach void close() throws Exception {if(service!=null)service.close();}
    RequestExperimentFromClient request() throws Exception {
        return mapper.readValue(Path.of("src/main/resources/examples/python-c.json").toFile(),RequestExperimentFromClient.class);
    }
    void setup(ModelExecutor... executors) throws Exception {
        var paths=new MainPaths();paths.setProjectRoot(root);paths.setExperimentsRoot(Path.of("runs"));
        service=new ExperimentService(new StartJsonFactory(mapper,new HardcodedModelProvider()),new ModelExecutorRegistry(List.of(executors)),new ExperimentFiles(mapper,paths));
    }
    interface Task {JsonNode run(ModelRequest m,ModelRunContext c) throws Exception;}
    ModelExecutor executor(ModelLanguage language,Task task) {
        return new ModelExecutor() {
            public boolean supports(ModelLanguage l){return l==language;}
            public JsonNode execute(ModelRequest m,ModelRunContext c) throws Exception{return task.run(m,c);}
        };
    }
    ExperimentStatus await(long id) throws Exception {
        for(int i=0;i<500;i++) {
            var s=service.status(id);
            if(Set.of("COMPLETED","FAILED","CANCELLED").contains(s.getStatus()))return s;
            Thread.sleep(10);
        }
        throw new AssertionError("Experiment did not finish");
    }
    @Test void asyncSingleActiveAndExplicitBinding() throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        var cCalled=new AtomicBoolean();
        setup(executor(ModelLanguage.PYTHON,(m,c)->{entered.countDown();release.await(5,TimeUnit.SECONDS);return mapper.valueToTree(Map.of("E_start",31.2,"E_end",31.2));}),
              executor(ModelLanguage.C,(m,c)->{assertThat(c.parameters).containsEntry("E_start",31.2).containsEntry("E_end",31.2);cCalled.set(true);return mapper.createObjectNode();}));
        var request=request();service.processExperiment(request);
        assertThat(entered.await(5,TimeUnit.SECONDS)).isTrue();
        assertThat(service.status(request.getExperimentId()).getStatus()).isEqualTo("RUNNING");
        assertThatThrownBy(()->service.processExperiment(request())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        release.countDown();
        assertThat(await(request.getExperimentId()).getStatus()).isEqualTo("COMPLETED");assertThat(cCalled).isTrue();
    }
    @Test void pythonFailureSkipsC() throws Exception {
        var called=new AtomicBoolean();
        setup(executor(ModelLanguage.PYTHON,(m,c)->{throw new java.io.IOException("MODEL_FAILED");}),
            executor(ModelLanguage.C,(m,c)->{called.set(true);return null;}));
        var r=request();service.processExperiment(r);var state=await(r.getExperimentId());
        assertThat(state.getStatus()).isEqualTo("FAILED");assertThat(state.getModelStatuses().get(2).getStatus()).isEqualTo("SKIPPED");assertThat(called).isFalse();
    }
    @Test void cancellationDoesNotStartNextModelOrOverwriteTerminalState() throws Exception {
        var entered=new CountDownLatch(1);var called=new AtomicBoolean();
        setup(executor(ModelLanguage.PYTHON,(m,c)->{entered.countDown();while(!c.cancelled.getAsBoolean())Thread.sleep(10);c.check();return null;}),
            executor(ModelLanguage.C,(m,c)->{called.set(true);return null;}));
        var r=request();service.processExperiment(r);assertThat(entered.await(5,TimeUnit.SECONDS)).isTrue();
        service.cancel(r.getExperimentId());assertThat(await(r.getExperimentId()).getStatus()).isEqualTo("CANCELLED");
        assertThat(service.cancel(r.getExperimentId()).getStatus()).isEqualTo("CANCELLED");assertThat(called).isFalse();
    }
    @Test void cancellingActiveCLeavesCompletedPythonSuccessful() throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        setup(executor(ModelLanguage.PYTHON,(m,c)->mapper.valueToTree(Map.of("E_start",31.2,"E_end",31.2))),
            executor(ModelLanguage.C,(m,c)->{entered.countDown();release.await(5,TimeUnit.SECONDS);c.check();return null;}));
        var r=request();service.processExperiment(r);assertThat(entered.await(5,TimeUnit.SECONDS)).isTrue();
        var cancelling=service.cancel(r.getExperimentId());
        assertThat(cancelling.getModelStatuses().get(1).getStatus()).isEqualTo("SUCCEEDED");
        assertThat(cancelling.getModelStatuses().get(2).getStatus()).isEqualTo("CANCELLING");
        release.countDown();var terminal=await(r.getExperimentId());
        assertThat(terminal.getStatus()).isEqualTo("CANCELLED");
        assertThat(terminal.getModelStatuses().get(1).getStatus()).isEqualTo("SUCCEEDED");
    }
    @Test void savedResultUrlsPointToSupervisorAfterContainerRemoval() throws Exception {
        setup();
        Path directory=root.resolve("runs/42");Files.createDirectories(directory.resolve("models/1"));
        var state=new ExperimentStatus();state.setExperimentId(42L);state.setStatus("COMPLETED");
        state.setModelStatuses(Map.of(1,new ExperimentStatus.ModelStatus()));
        mapper.writeValue(directory.resolve("status.json").toFile(),state);
        mapper.writeValue(directory.resolve("models/1/metadata.json").toFile(),Map.of("downloadUrl","/api/v1/runs/old/result"));
        var published=service.results(42).getFirst();
        assertThat(published.path("metadata").path("downloadUrl").asText()).isEqualTo("/api/experiments/42/models/1/result");
        assertThat(published.path("resultUrl").asText()).isEqualTo("/api/experiments/42/models/1/result");
    }
    @Test void conflictRejectedBeforeRegistration() throws Exception {
        setup();var r=request();r.getModels().get(1).getParameters().put("E_start",30);
        assertThatThrownBy(()->service.processExperiment(r)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("conflicting");
    }
}
