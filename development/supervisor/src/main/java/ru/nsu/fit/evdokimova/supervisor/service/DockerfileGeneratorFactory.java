package ru.nsu.fit.evdokimova.supervisor.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.nsu.fit.evdokimova.supervisor.model.ModelLanguage;
import ru.nsu.fit.evdokimova.supervisor.utils.DockerGeneration.IDockerfileGenerator;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DockerfileGeneratorFactory {
    private final Map<ModelLanguage, IDockerfileGenerator> generators;

    @Autowired
    public DockerfileGeneratorFactory(List<IDockerfileGenerator> generatorList) {
        generators = generatorList.stream()
                .collect(Collectors.toMap(
                        this::getLanguageFromGenerator,
                        Function.identity()
                ));
    }

    public IDockerfileGenerator getGenerator(ModelLanguage language) {
        return Optional.ofNullable(generators.get(language))
                .orElseThrow(() -> new IllegalArgumentException("No generator for language: " + language));
    }

    private ModelLanguage getLanguageFromGenerator(IDockerfileGenerator generator) {
        String className = generator.getClass().getSimpleName();
        String languageName = className.replace("DockerGenerator", "").toUpperCase();
        return ModelLanguage.valueOf(languageName);
    }
}