package ru.nsu.fit.evdokimova.supervisor.service.execmodels;

import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.Value;
import ru.nsu.fit.evdokimova.supervisor.configuration.MainPaths;
import ru.nsu.fit.evdokimova.supervisor.model.*;
import ru.nsu.fit.evdokimova.supervisor.service.runtime.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Docker is the environment; only the model HTTP API determines calculation success. */
public abstract class AbstractDockerModelExecutor implements ModelExecutor {
    protected final MainPaths paths;
    protected final ExperimentFiles files;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    @Value("${supervisor.runtime.readiness-seconds:60}") private long readinessSeconds=60;
    @Value("${supervisor.runtime.expected-seconds:600}") private long expectedSeconds=600;
    @Value("${supervisor.runtime.poll-millis:200}") private long pollMillis=200;
    protected AbstractDockerModelExecutor(MainPaths paths, ExperimentFiles files) { this.paths=paths; this.files=files; }

    protected void validatePackage(ModelRequest model) throws Exception { }

    private class Session {
        final ModelRequest model; final ModelRunContext ctx; final String name, runId;
        String base; boolean created=false; long logs=0, publications=0;
        Session(ModelRequest model, ModelRunContext ctx) {
            this.model=model; this.ctx=ctx;
            name="skif-exp-"+ctx.experimentId+"-"+model.getOrder()+"-"+UUID.randomUUID().toString().substring(0,8);
            runId="exp-"+ctx.experimentId+"-model-"+model.getOrder();
        }
        Path file(String name) { return ctx.directory.resolve(name); }
        int command(List<String> args, Path output, boolean cancellable) throws Exception {
            if (cancellable) ctx.check();
            Files.createDirectories(output.getParent());
            ctx.event.accept("Docker command: " + String.join(" ", args));
            Process p=new ProcessBuilder(args).directory(paths.getProjectRoot().toAbsolutePath().toFile())
                    .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(output.toFile())).start();
            try {
                while (!p.waitFor(100, TimeUnit.MILLISECONDS)) {
                    if (cancellable) ctx.check();
                }
                if (cancellable) ctx.check();
                return p.exitValue();
            } finally {
                if (p.isAlive()) { p.destroy(); if (!p.waitFor(2,TimeUnit.SECONDS)) {p.destroyForcibly(); p.waitFor();} }
            }
        }
        String docker(boolean cancellable, String... args) throws Exception {
            Path output=Files.createTempFile(ctx.directory,"docker-", ".log");
            List<String> command=new ArrayList<>(List.of("docker")); command.addAll(List.of(args));
            int code=command(command,output,cancellable);
            String text=Files.readString(output);
            files.append(file("docker-commands.log"),String.join(" ",command)+"\nexit="+code+"\n"+text);
            Files.delete(output);
            if (code!=0) throw new IOException("DOCKER_ERROR: "+String.join(" ",args)+": "+text);
            return text.trim();
        }
        byte[] api(String method, String path, Object body, boolean success) throws Exception {
            HttpRequest.Builder builder=HttpRequest.newBuilder(URI.create(base+"/api/v1"+path)).timeout(Duration.ofSeconds(5));
            if (method.equals("POST")) builder.header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(files.mapper.writeValueAsBytes(body)));
            else builder.GET();
            HttpResponse<byte[]> response;
            try { response=http.send(builder.build(),HttpResponse.BodyHandlers.ofByteArray()); }
            catch (IOException e) {
                String kind="API_UNREACHABLE";
                try { if (!docker(false,"inspect","--format","{{.State.Running}}",name).equals("true")) kind="CONTAINER_EXITED"; }
                catch (Exception ignored) { }
                throw new IOException(kind+": "+e.getMessage(),e);
            }
            if (success && response.statusCode()/100!=2)
                throw new IOException("MODEL_HTTP_"+response.statusCode()+": "+new String(response.body(),StandardCharsets.UTF_8));
            return response.body();
        }
        JsonNode get(String path) throws Exception { return files.mapper.readTree(api("GET",path,null,true)); }
        String runPath() {return "/runs/"+runId;}
        void journals(String endpoint) throws Exception {
            long cursor=endpoint.equals("logs")?logs:publications;
            while (true) {
                JsonNode page=get(runPath()+"/"+endpoint+"?after="+cursor+"&limit=100");
                if (!page.path("items").isArray()) throw new IOException("MODEL_PROTOCOL_ERROR: journal items missing");
                for (JsonNode item:page.get("items")) {
                    long seq=item.path("seq").asLong();
                    if (seq<=cursor) throw new IOException("MODEL_PROTOCOL_ERROR: invalid journal sequence");
                    files.append(file(endpoint+".jsonl"),files.mapper.writeValueAsString(item)); cursor=seq;
                    if (endpoint.equals("logs")) logs=cursor; else publications=cursor;
                }
                if (!page.path("hasMore").asBoolean()) break;
            }
        }
        void collect() throws Exception { journals("logs"); journals("publications"); }
        void cancelCalculation() {
            if (base==null) return;
            try {
                api("POST",runPath()+"/cancel",Map.of(),true);
                long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
                while (System.nanoTime()<end) {
                    JsonNode status=get(runPath()); files.write(file("model-status.json"),status); collect();
                    if (Set.of("CANCELLED","SUCCEEDED","FAILED").contains(status.path("status").asText())) return;
                    Thread.sleep(100);
                }
            } catch (Exception e) { ctx.event.accept("HTTP cancellation fallback: "+e.getMessage()); }
        }
        void cleanup() throws Exception {
            if (!created) return;
            ctx.cleanupConfirmed=false;
            String inspection;
            try { inspection=docker(false,"inspect",name); }
            catch(IOException e) {
                if (e.getMessage().contains("No such object") || e.getMessage().contains("No such container")) {
                    ctx.cleanupConfirmed=true; return;
                }
                throw e;
            }
            JsonNode owner=files.mapper.readTree(inspection).get(0).path("Config").path("Labels").path("skif.supervisor.experiment");
            if (!owner.asText().equals(Long.toString(ctx.experimentId))) throw new IOException("STOP_FAILED: refusing foreign container "+name);
            // Best effort HTTP journals before stopping the API; preserve all previously saved pages.
            if (base!=null) try { collect(); } catch (Exception e) { ctx.event.accept("Final journal read: "+e.getMessage()); }
            try { files.bytes(file("container-inspect.json"),docker(false,"inspect",name).getBytes(StandardCharsets.UTF_8)); }
            catch (Exception e) { ctx.event.accept("Inspect: "+e.getMessage()); }
            try { docker(false,"logs",name); } catch (Exception e) { ctx.event.accept("Docker logs: "+e.getMessage()); }
            try { docker(false,"cp",name+":/runs",file("wrapper-files").toString()); }
            catch (Exception e) { ctx.event.accept("Diagnostic copy: "+e.getMessage()); }
            try { docker(false,"stop","--time","3",name); }
            catch (Exception e) { docker(false,"kill",name); }
            if (!docker(false,"inspect","--format","{{.State.Running}}",name).equals("false"))
                throw new IOException("STOP_FAILED: container still running "+name);
            docker(false,"rm",name);
            ctx.cleanupConfirmed=true;
            files.write(file("cleanup.json"),Map.of("container",name,"removed",true));
        }
        JsonNode run() throws Exception {
            ctx.stage("BUILDING");
            validatePackage(model);
            Path root=paths.getProjectRoot().toAbsolutePath().normalize();
            Path iid=file("image.id");
            String dockerfile="models/"+model.getModelPath()+"/Dockerfile.api";
            int code=command(List.of("docker","build","--iidfile",iid.toString(),"-f",dockerfile,
                "-t","skif-model-api:exp-"+ctx.experimentId+"-"+model.getOrder(),root.toString()),file("build.log"),true);
            if (code!=0) throw new IOException("BUILD_FAILED: exit="+code+"; see build.log");
            ctx.stage("STARTING_CONTAINER");
            // Known unique name also allows cleanup if cancellation races Docker create.
            created=true;
            docker(false,"create","--init","--restart=no","--name",name,"--label",
                "skif.supervisor.experiment="+ctx.experimentId,"-p","127.0.0.1::8080",Files.readString(iid).trim());
            ctx.check();
            docker(true,"start",name);
            base="http://"+docker(true,"port",name,"8080/tcp");
            files.write(file("container.json"),Map.of("name",name,"baseUrl",base,"runId",runId));
            ctx.stage("WAITING_API");
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(readinessSeconds);
            boolean ready=false;
            while (System.nanoTime()<deadline) {
                ctx.check();
                try { if (get("/readiness").path("ready").asBoolean()) {ready=true;break;} }
                catch (IOException error) {
                    if (error.getMessage().startsWith("CONTAINER_EXITED")) throw error;
                    files.write(file("readiness-error.json"),Map.of("error",error.getMessage()));
                }
                Thread.sleep(pollMillis);
            }
            if (!ready) throw new IOException("API_NOT_READY: readiness deadline exceeded");
            ctx.stage("SUBMITTING");
            Map<String,Object> request=Map.of("runId",runId,"experimentId",Long.toString(ctx.experimentId),"parameters",ctx.parameters);
            files.write(file("request.json"),request);
            ctx.check();
            JsonNode accepted=files.mapper.readTree(api("POST","/runs",request,true));
            if (!runId.equals(accepted.path("runId").asText())) throw new IOException("MODEL_PROTOCOL_ERROR: runId mismatch");
            long started=System.nanoTime(); boolean warned=false;
            while (true) {
                ctx.check();
                JsonNode state=get(runPath()); files.write(file("model-status.json"),state);
                if (!runId.equals(state.path("runId").asText())) throw new IOException("MODEL_PROTOCOL_ERROR: runId mismatch");
                collect();
                String value=state.path("status").asText();
                ctx.stage("MODEL_"+value);
                if (value.equals("FAILED")) throw new IOException("MODEL_FAILED: "+state.path("error"));
                if (value.equals("CANCELLED")) throw new IOException("MODEL_CANCELLED unexpectedly");
                if (value.equals("SUCCEEDED")) break;
                if (!Set.of("ACCEPTED","RUNNING","FINALIZING","CANCELLING").contains(value))
                    throw new IOException("MODEL_PROTOCOL_ERROR: unknown status "+value);
                if (!warned && System.nanoTime()-started>TimeUnit.SECONDS.toNanos(expectedSeconds)) {
                    warned=true; ctx.event.accept("WARNING EXPECTED_DURATION_EXCEEDED: calculation continues");
                    files.write(file("warning.json"),Map.of("code","EXPECTED_DURATION_EXCEEDED"));
                }
                Thread.sleep(pollMillis);
            }
            ctx.stage("SAVING_RESULT");
            JsonNode metadata=get(runPath()+"/result/metadata");
            byte[] payload=api("GET",runPath()+"/result",null,true);
            String digest=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload));
            if (metadata.path("sizeBytes").asLong(-1)!=payload.length || !digest.equals(metadata.path("sha256").asText()))
                throw new IOException("RESULT_INVALID: size or SHA-256 mismatch");
            JsonNode result=files.mapper.readTree(payload);
            validateResult(model.getLanguage(),result);
            if (model.getLanguage()==ModelLanguage.UNITY) {
                for(String parameter:List.of("angleDeg","speedDegPerSec")) {
                    if(Double.compare(((Number)ctx.parameters.get(parameter)).doubleValue(),result.path(parameter).asDouble())!=0)
                        throw new IOException("RESULT_INVALID: Unity returned different "+parameter);
                }
            }
            ctx.check();
            files.bytes(file("result.json"),payload); files.write(file("metadata.json"),metadata);
            collect();
            return result;
        }
    }
    @Override public JsonNode execute(ModelRequest model, ModelRunContext ctx) throws Exception {
        Files.createDirectories(ctx.directory);
        for (String journal:List.of("logs.jsonl","publications.jsonl")) {
            Path target=ctx.directory.resolve(journal);
            if(!Files.exists(target)) Files.createFile(target);
        }
        Session session=new Session(model,ctx);
        try { return session.run(); }
        finally {
            if (ctx.cancelled.getAsBoolean()) session.cancelCalculation();
            session.cleanup();
        }
    }
    public static void validateResult(ModelLanguage language, JsonNode result) throws IOException {
        if (result==null || !result.isObject()) throw new IOException("RESULT_INVALID: expected object");
        if (language==ModelLanguage.UNITY) {
            UnityModelExecutor.validateUnityResult(result);
            return;
        }
        for (String field:language==ModelLanguage.PYTHON?new String[]{"E_start","E_end"}:new String[]{"max_dA","min_dA"})
            finite(result.path(field),field);
        if (language==ModelLanguage.C) {
            JsonNode matrix=result.path("img");
            if (!matrix.isArray() || matrix.size()!=988) throw new IOException("RESULT_INVALID: img rows");
            for (JsonNode row:matrix) {
                if (!row.isArray() || row.size()!=988) throw new IOException("RESULT_INVALID: img columns");
                for (JsonNode number:row) finite(number,"img");
            }
            for (String field:new String[]{"max_err_abs","mean","max_err","min_err","inters_err"}) finite(result.path("error_rate").path(field),field);
        }
    }
    private static void finite(JsonNode node,String field) throws IOException {
        if (!node.isNumber() || !Double.isFinite(node.doubleValue())) throw new IOException("RESULT_INVALID: non-finite "+field);
    }
}
