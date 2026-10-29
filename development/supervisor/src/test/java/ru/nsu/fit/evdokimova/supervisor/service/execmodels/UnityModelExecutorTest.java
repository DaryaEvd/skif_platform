package ru.nsu.fit.evdokimova.supervisor.service.execmodels;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.nsu.fit.evdokimova.supervisor.configuration.MainPaths;
import ru.nsu.fit.evdokimova.supervisor.service.runtime.ExperimentFiles;
import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class UnityModelExecutorTest {
    @TempDir Path root;
    @Test void fractionalParametersPreservedAndBoundsChecked() {
        UnityModelExecutor.validateParameters(Map.of("angleDeg",15.5,"speedDegPerSec",7.75));
        for(double angle:new double[]{0,.005,.01,361,Double.NaN})
            assertThatThrownBy(()->UnityModelExecutor.validateParameters(Map.of("angleDeg",angle,"speedDegPerSec",7.75)))
                .isInstanceOf(IllegalArgumentException.class);
        for(double speed:new double[]{0,-1,36.1,Double.POSITIVE_INFINITY})
            assertThatThrownBy(()->UnityModelExecutor.validateParameters(Map.of("angleDeg",15.5,"speedDegPerSec",speed)))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void absentOrOldBuildIsRejectedWithoutLaunchingEditor() throws Exception {
        var paths=new MainPaths();paths.setProjectRoot(root);
        var executor=new UnityModelExecutor(paths,new ExperimentFiles(new ObjectMapper(),paths));
        assertThatThrownBy(()->executor.validatePackage(null)).hasMessageContaining("UNITY_BUILD_MISSING");
        Path build=root.resolve("models/model_03/Builds/ModelApi");Files.createDirectories(build);
        Files.writeString(build.resolve("model-api-build.json"),"{\"scenario\":\"old\",\"unityVersion\":\"2021\"}");
        assertThatThrownBy(()->executor.validatePackage(null)).hasMessageContaining("UNITY_BUILD_INVALID");
    }
    @Test void orientationAndAccumulatedRotationMustBeValid() throws Exception {
        var mapper=new ObjectMapper();
        var result=mapper.readTree("""
            {"angleDeg":15.5,"speedDegPerSec":7.75,"accumulatedRotationDeg":15.499,
             "remainingAngleDeg":0.001,"toleranceDeg":0.01,"orientationErrorDeg":0.001,
             "modelElapsedSeconds":2.01,"realElapsedSeconds":2.1,
             "initialLocalOrientation":{"x":0,"y":0,"z":0,"w":1},
             "finalLocalOrientation":{"x":0,"y":0,"z":0,"w":1}}
            """);
        UnityModelExecutor.validateUnityResult(result);
        ((com.fasterxml.jackson.databind.node.ObjectNode)result).put("accumulatedRotationDeg",0);
        assertThatThrownBy(()->UnityModelExecutor.validateUnityResult(result)).hasMessageContaining("RESULT_INVALID");
    }
}
