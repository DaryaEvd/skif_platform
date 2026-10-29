package ru.nsu.fit.evdokimova.supervisor.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ru.nsu.fit.evdokimova.supervisor.model.*;
import ru.nsu.fit.evdokimova.supervisor.service.execmodels.*;
import ru.nsu.fit.evdokimova.supervisor.service.runtime.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ExperimentService {
    private final StartJsonFactory factory;
    private final ModelExecutorRegistry registry;
    private final ExperimentFiles files;
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"experiment-worker");t.setDaemon(true);return t;});
    private final Map<Long,Run> runs=new HashMap<>();
    private Run active;
    private static final Set<String> TERMINAL=Set.of("COMPLETED","FAILED","CANCELLED");
    private static class Run {
        final RequestExperimentFromClient config;
        final ExperimentStatus status=new ExperimentStatus();
        final AtomicBoolean cancelled=new AtomicBoolean();
        final Path directory;
        volatile ModelRunContext context;
        Run(RequestExperimentFromClient config,Path directory) {this.config=config;this.directory=directory;}
    }
    public ExperimentService(StartJsonFactory factory,ModelExecutorRegistry registry,ExperimentFiles files) throws IOException {
        this.factory=factory;this.registry=registry;this.files=files;
        // No resumption: mark interrupted histories explicitly instead of displaying eternal RUNNING.
        try(var dirs=Files.list(files.root)) {
            for(Path dir:dirs.filter(Files::isDirectory).toList()) {
                Path saved=dir.resolve("status.json");
                if(Files.exists(saved)) {
                    ExperimentStatus status=files.mapper.readValue(saved.toFile(),ExperimentStatus.class);
                    if(!TERMINAL.contains(status.getStatus())) {
                        status.setStatus("FAILED"); status.setStage("INTERRUPTED");
                        status.setErrorMessage("SERVER_RESTARTED: no automatic resumption; inspect any orphan containers");
                        status.setLastUpdated(LocalDateTime.now());
                        for(var model:status.getModelStatuses().values()) {
                            if(!Set.of("SUCCEEDED","FAILED","CANCELLED","SKIPPED").contains(model.getStatus())) {
                                model.setStatus("SKIPPED");model.setStage("INTERRUPTED");model.setErrorMessage(status.getErrorMessage());
                            }
                        }
                        files.write(saved,status);
                    }
                }
            }
        }
    }
    /** Existing service entry point, now registers and returns without waiting for Docker/calculation. */
    public synchronized void processExperiment(RequestExperimentFromClient request) throws IOException {
        validate(request);
        if(active!=null) throw new ResponseStatusException(HttpStatus.CONFLICT,"An experiment is already active (or cleanup is unresolved)");
        request.generateId();
        RequestExperimentFromClient config=files.mapper.convertValue(request,RequestExperimentFromClient.class);
        config.setModels(config.getModels().stream().sorted(Comparator.comparingInt(ModelRequest::getOrder)).toList());
        Path directory=files.experiment(config.getExperimentId());Files.createDirectory(directory);
        Run run=new Run(config,directory);
        var status=run.status;
        status.setExperimentId(config.getExperimentId());status.setTotalModels(config.getModels().size());
        status.setStatus("RUNNING");status.setStage("REGISTERED");status.setModelStatuses(new LinkedHashMap<>());
        for(ModelRequest model:config.getModels()) {
            var modelStatus=new ExperimentStatus.ModelStatus();
            modelStatus.setModelId(model.getModelId());modelStatus.setModelName(model.getName());
            modelStatus.setRunId("exp-"+config.getExperimentId()+"-model-"+model.getOrder());
            modelStatus.setStatus("PENDING");modelStatus.setStage("PENDING");
            status.getModelStatuses().put(model.getOrder(),modelStatus);
        }
        files.write(directory.resolve("configuration.json"),config);
        save(run);runs.put(config.getExperimentId(),run);active=run;
        files.event(directory,"Experiment registered");
        worker.submit(()->execute(run));
    }
    private void validate(RequestExperimentFromClient request) {
        if(request==null || request.getExperimentName()==null || request.getExperimentName().isBlank()
            || request.getModels()==null || request.getModels().isEmpty()) throw new IllegalArgumentException("experimentName and nonempty models required");
        Set<Integer> orders=new HashSet<>();Set<String> ids=new HashSet<>();
        for(ModelRequest model:request.getModels()) {
            if(model==null || model.getOrder()==null || model.getOrder()<1 || !orders.add(model.getOrder())
               || model.getModelId()==null || !model.getModelId().matches("[A-Za-z0-9_-]{1,100}") || !ids.add(model.getModelId())
               || model.getName()==null || model.getParameters()==null) throw new IllegalArgumentException("Each model needs unique modelId/order, name and parameters");
            if(model.getLanguage()!=ModelLanguage.PYTHON && model.getLanguage()!=ModelLanguage.C)
                throw new IllegalArgumentException("Only existing PYTHON and C executors are supported");
            String expected=model.getLanguage()==ModelLanguage.PYTHON?"model_01":"model_02";
            if(model.getModelPath()==null || !model.getModelPath().toString().equals(expected))
                throw new IllegalArgumentException("modelPath must be "+expected+" for "+model.getLanguage());
        }
        var ordered=request.getModels().stream().sorted(Comparator.comparingInt(ModelRequest::getOrder)).toList();
        for(int i=1;i<ordered.size();i++) {
            if(ordered.get(i).getLanguage()==ModelLanguage.C && ordered.get(i-1).getLanguage()==ModelLanguage.PYTHON
                && (ordered.get(i).getParameters().containsKey("E_start") || ordered.get(i).getParameters().containsKey("E_end")))
                throw new IllegalArgumentException("C E_start/E_end are bound to Python output; remove conflicting static parameters");
        }
    }
    private synchronized void update(Run run,ModelRequest model,String state,String stage) {
        var ms=run.status.getModelStatuses().get(model.getOrder());
        if(run.cancelled.get()) {run.status.setStatus("CANCELLING");run.status.setStage("CANCELLING");}
        else {ms.setStatus(state);ms.setStage(stage);run.status.setStage(stage);}
        run.status.setCurrentModel(model.getName());run.status.setCurrentOrder(model.getOrder());
        persist(run);
    }
    private void execute(Run run) {
        ModelRequest current=null; JsonNode previous=null; ModelLanguage previousLanguage=null;
        try {
            for(ModelRequest model:run.config.getModels()) {
                synchronized(this) {
                    check(run);current=model;
                    var ms=run.status.getModelStatuses().get(model.getOrder());ms.setStartTime(LocalDateTime.now());
                    update(run,model,"RUNNING","PREPARING");
                }
                Map<String,Object> output=previous!=null && previousLanguage==ModelLanguage.PYTHON && model.getLanguage()==ModelLanguage.C
                    ?files.mapper.convertValue(previous,new TypeReference<Map<String,Object>>(){}):null;
                Map<String,Object> parameters=factory.create(model,output);
                Path dir=run.directory.resolve("models").resolve(Integer.toString(model.getOrder()));Files.createDirectories(dir);
                files.write(dir.resolve("parameters.json"),parameters);
                ModelRunContext context=new ModelRunContext(run.config.getExperimentId(),dir,parameters,run.cancelled::get,
                    (state,stage)->update(run,model,state,stage),message->files.event(run.directory,model.getName()+": "+message));
                run.context=context;
                JsonNode result=registry.get(model.getLanguage()).execute(model,context);
                synchronized(this) {
                    check(run);
                    var ms=run.status.getModelStatuses().get(model.getOrder());ms.setStatus("SUCCEEDED");
                    ms.setStage("COMPLETED");ms.setResultAvailable(true);ms.setEndTime(LocalDateTime.now());save(run);
                }
                previous=result;previousLanguage=model.getLanguage();
            }
            synchronized(this) {check(run);run.status.setStatus("COMPLETED");run.status.setStage("COMPLETED");save(run);}
        } catch(Exception error) {
            synchronized(this) {
                boolean cancelled=run.cancelled.get() && (run.context==null || run.context.cleanupConfirmed);
                run.status.setStatus(cancelled?"CANCELLED":"FAILED");run.status.setStage(cancelled?"CANCELLED":"FAILED");
                run.status.setErrorMessage(cancelled?null:error.getMessage());
                if(current!=null) {
                    var ms=run.status.getModelStatuses().get(current.getOrder());
                    if (!ms.getStatus().equals("SUCCEEDED")) {
                        ms.setStatus(cancelled?"CANCELLED":"FAILED");ms.setStage(ms.getStatus());
                        ms.setErrorMessage(cancelled?null:error.getMessage());ms.setEndTime(LocalDateTime.now());
                        ms.setResultAvailable(Files.exists(run.directory.resolve("models/"+current.getOrder()+"/result.json")));
                    }
                }
                for(var ms:run.status.getModelStatuses().values()) if(ms.getStatus().equals("PENDING")) {ms.setStatus("SKIPPED");ms.setStage("SKIPPED");}
                persist(run);files.event(run.directory,"Experiment "+run.status.getStatus()+": "+error);
            }
        } finally {
            synchronized(this) {
                if(run.context==null || run.context.cleanupConfirmed) active=null;
                else files.event(run.directory,"WARNING: active slot retained because container cleanup is unresolved");
            }
        }
    }
    private void check(Run run) {if(run.cancelled.get()) throw new CancellationException("Experiment cancelled");}
    private void save(Run run) throws IOException {run.status.setLastUpdated(LocalDateTime.now());files.write(run.directory.resolve("status.json"),run.status);}
    private void persist(Run run) {try{save(run);}catch(IOException e){throw new UncheckedIOException(e);}}
    public synchronized ExperimentStatus status(long id) throws IOException {
        Path path=files.experiment(id).resolve("status.json");
        if(!Files.exists(path)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Experiment not found");
        return files.mapper.readValue(path.toFile(),ExperimentStatus.class);
    }
    public synchronized ExperimentStatus cancel(long id) throws IOException {
        ExperimentStatus status=status(id);Run run=runs.get(id);
        if(TERMINAL.contains(status.getStatus())) return status;
        if(run==null) throw new ResponseStatusException(HttpStatus.CONFLICT,"Experiment is not running in this server");
        run.cancelled.set(true);run.status.setStatus("CANCELLING");run.status.setStage("CANCELLING");
        var model=run.status.getModelStatuses().get(run.status.getCurrentOrder());
        if(model!=null && model.getStatus().equals("RUNNING")) {model.setStatus("CANCELLING");model.setStage("CANCELLING");}
        save(run);
        files.event(run.directory,"Cancellation requested; next model prohibited");return status(id);
    }
    public JsonNode configuration(long id) throws IOException {status(id);return files.read(files.experiment(id).resolve("configuration.json"));}
    public Path modelFile(long id,int order,String file) throws IOException {
        ExperimentStatus status=status(id);
        if(!status.getModelStatuses().containsKey(order)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Model order not found");
        Path path=files.experiment(id).resolve("models/"+order+"/"+file);
        if(!Files.exists(path)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Data not available yet; inspect experiment status");
        return path;
    }
    public List<JsonNode> results(long id) throws IOException {
        var result=new ArrayList<JsonNode>();
        for(Integer order:status(id).getModelStatuses().keySet()) {
            Path metadata=files.experiment(id).resolve("models/"+order+"/metadata.json");
            if(Files.exists(metadata)) {
                String url="/api/experiments/"+id+"/models/"+order+"/result";
                var published=files.read(metadata).deepCopy();
                ((com.fasterxml.jackson.databind.node.ObjectNode)published).put("downloadUrl",url);
                var item=files.mapper.createObjectNode();item.put("order",order);item.set("metadata",published);
                item.put("resultUrl",url);result.add(item);
            }
        }
        return result;
    }
    public Map<String,Object> logs(long id,int after,int limit) throws IOException {
        if(after<0 || limit<1 || limit>1000) throw new IllegalArgumentException("after >= 0 and limit 1..1000 required");
        Path path=files.experiment(id).resolve("events.log");ExperimentStatus status=status(id);
        List<String> lines=Files.exists(path)?Files.readAllLines(path):List.of();
        int start=Math.min(after,lines.size()),end=Math.min(start+limit,lines.size());
        return Map.of("items",lines.subList(start,end),"nextAfter",end,"hasMore",end<lines.size(),"closed",TERMINAL.contains(status.getStatus()));
    }
    @PreDestroy public void close() throws InterruptedException {
        synchronized(this) {if(active!=null)active.cancelled.set(true);}
        worker.shutdown();worker.awaitTermination(30,TimeUnit.SECONDS);
    }
}
