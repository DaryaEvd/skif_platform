package ru.nsu.fit.evdokimova.supervisor.service;

import org.springframework.stereotype.Component;
import ru.nsu.fit.evdokimova.supervisor.model.ModelLanguage;

import java.util.List;

@Component
public class ModelExecutorRegistry {

    private final List<ModelExecutor> executors;

    public ModelExecutorRegistry(List<ModelExecutor> executors) {
        this.executors = executors;
    }

    public ModelExecutor get(ModelLanguage language) {
        return executors.stream()
                .filter(e -> e.supports(language))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("No executor for " + language)
                );
    }

}
