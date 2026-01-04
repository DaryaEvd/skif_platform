package ru.nsu.fit.evdokimova.supervisor.service;

import ru.nsu.fit.evdokimova.supervisor.model.ModelLanguage;
import ru.nsu.fit.evdokimova.supervisor.model.ModelRequest;

import java.nio.file.Path;

public interface ModelExecutor {
    boolean supports(ModelLanguage language);
    Path execute(
            ModelRequest model,
            Path startFile,
            Path endDir,
            Path modelJsonDir
    ) throws Exception;

}
