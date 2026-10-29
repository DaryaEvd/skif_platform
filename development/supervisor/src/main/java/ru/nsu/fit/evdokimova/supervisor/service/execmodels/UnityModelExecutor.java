package ru.nsu.fit.evdokimova.supervisor.service.execmodels;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import ru.nsu.fit.evdokimova.supervisor.configuration.MainPaths;
import ru.nsu.fit.evdokimova.supervisor.model.*;
import ru.nsu.fit.evdokimova.supervisor.service.runtime.ExperimentFiles;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

@Component
public class UnityModelExecutor extends AbstractDockerModelExecutor {
    public UnityModelExecutor(MainPaths paths,ExperimentFiles files) {super(paths,files);}
    @Override public boolean supports(ModelLanguage language) {return language==ModelLanguage.UNITY;}
    public static void validateParameters(Map<String,Object> params) {
        if(!params.keySet().equals(Set.of("angleDeg","speedDegPerSec")))
            throw new IllegalArgumentException("Unity requires only angleDeg and speedDegPerSec");
        for(String key:params.keySet()) {
            Object value=params.get(key);
            if(!(value instanceof Number n) || !Double.isFinite(n.doubleValue()) || !Float.isFinite(n.floatValue()) || n.floatValue()<=0)
                throw new IllegalArgumentException("Unity "+key+" must be a positive finite float32 number");
        }
        double angle=((Number)params.get("angleDeg")).doubleValue(),speed=((Number)params.get("speedDegPerSec")).doubleValue();
        if(angle<=0.01 || angle>360 || speed<=0 || speed>36)
            throw new IllegalArgumentException("Unity: 0.01 < angleDeg <= 360; 0 < speedDegPerSec <= 36");
    }
    private static String hash(Path path) throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        try(InputStream stream=Files.newInputStream(path)) {byte[] buffer=new byte[65536];int n;while((n=stream.read(buffer))!=-1)digest.update(buffer,0,n);}
        return HexFormat.of().formatHex(digest.digest());
    }
    private static Path within(Path root,String relative) throws IOException {
        Path path=root.resolve(relative).normalize();
        if(!path.startsWith(root) || !Files.isRegularFile(path)) throw new IOException("UNITY_BUILD_INVALID: missing "+relative);
        return path;
    }
    @Override protected void validatePackage(ModelRequest model) throws Exception {
        Path project=paths.getProjectRoot().toAbsolutePath().normalize().resolve("models/model_03");
        Path build=project.resolve("Builds/ModelApi");Path manifest=build.resolve("model-api-build.json");
        if(!Files.isRegularFile(manifest)) throw new IOException("UNITY_BUILD_MISSING: build Linux with BuildModelApi.Linux; see README");
        JsonNode data=files.read(manifest);
        if(!data.path("scenario").asText().equals("phi-relative-v1") || !data.path("unityVersion").asText().equals("6000.5.5f1") || !data.path("subtarget").asText().equals("DedicatedServer"))
            throw new IOException("UNITY_BUILD_INVALID: scenario or Editor version mismatch");
        JsonNode sources=data.path("sources"),binaries=data.path("files");
        for(String required:List.of("Assets/Object/Script/UnityModelApi.cs","Assets/Object/Script/UnityServerMode.cs","Assets/Object/Script/UnityHttpServer.cs","Assets/Object/Script/MoveObj.cs","Assets/Object/Script/NewMoveComponent.cs","Assets/Object/Script/RayTarget.cs","Assets/Scenes/SampleScene.unity","Assets/Editor/BuildModelApi.cs"))
            if(!sources.has(required)) throw new IOException("UNITY_BUILD_INVALID: source manifest missing "+required);
        for(String required:List.of("model.x86_64","UnityPlayer.so","model_Data/Managed/Assembly-CSharp.dll"))
            if(!binaries.has(required)) throw new IOException("UNITY_BUILD_INVALID: binary manifest missing "+required);
        for(var entry:sources.properties()) if(!hash(within(project,entry.getKey())).equals(entry.getValue().asText()))
            throw new IOException("UNITY_BUILD_STALE: rebuild after changing "+entry.getKey());
        for(var entry:binaries.properties()) if(!hash(within(build,entry.getKey())).equals(entry.getValue().asText()))
            throw new IOException("UNITY_BUILD_INVALID: binary hash mismatch "+entry.getKey());
    }
    public static void validateUnityResult(JsonNode result) throws IOException {
        for(String key:List.of("angleDeg","speedDegPerSec","accumulatedRotationDeg","remainingAngleDeg","toleranceDeg","modelElapsedSeconds","realElapsedSeconds","orientationErrorDeg"))
            if(!result.path(key).isNumber() || !Double.isFinite(result.get(key).asDouble())) throw new IOException("RESULT_INVALID: Unity "+key);
        double tolerance=result.get("toleranceDeg").asDouble(),angle=result.get("angleDeg").asDouble(),actual=result.get("accumulatedRotationDeg").asDouble();
        if(Math.abs(tolerance-0.01)>1e-9 || angle<=0.01 || angle>360 || actual<=0 || Math.abs(angle-actual)>tolerance ||
            Math.abs(result.get("remainingAngleDeg").asDouble())>tolerance || (result.get("orientationErrorDeg").asDouble()<0 || result.get("orientationErrorDeg").asDouble()>tolerance) ||
            result.get("modelElapsedSeconds").asDouble()<=0 || result.get("realElapsedSeconds").asDouble()<0 ||
            result.get("speedDegPerSec").asDouble()<=0 || result.get("speedDegPerSec").asDouble()>36)
            throw new IOException("RESULT_INVALID: Unity movement verification");
        for(String key:List.of("initialLocalOrientation","finalLocalOrientation")) {
            JsonNode q=result.path(key);double norm=0;
            for(String axis:List.of("x","y","z","w")) {
                if(!q.path(axis).isNumber() || !Double.isFinite(q.get(axis).asDouble()))throw new IOException("RESULT_INVALID: quaternion "+key);
                norm+=q.get(axis).asDouble()*q.get(axis).asDouble();
            }
            if(Math.abs(norm-1)>1e-4)throw new IOException("RESULT_INVALID: non-unit quaternion");
        }
    }
}
