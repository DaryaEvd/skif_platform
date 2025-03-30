package ru.nsu.fit.evdokimova.supervisor.utils;

import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class LocalModelLoader implements IModelLoader {
//    private static final String LOCAL_MODELS_PATH = "/home/darya/models/";

    @Override
    public String loadModel(String modelName, String version) {
//        String modelPath = LOCAL_MODELS_PATH + modelName + "/" + version;
        String modelPath = "/home/darya/RiderProjects/Step1/Step1";
        if (Files.exists(Paths.get(modelPath))) {
            return modelPath;
        } else {
            throw new RuntimeException("Model not found on the path: " + modelPath);
        }
    }
}
