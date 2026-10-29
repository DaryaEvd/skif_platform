package ru.nsu.fit.evdokimova.supervisor.service.execmodels;
import org.springframework.stereotype.Component;
import ru.nsu.fit.evdokimova.supervisor.configuration.MainPaths;
import ru.nsu.fit.evdokimova.supervisor.model.ModelLanguage;
import ru.nsu.fit.evdokimova.supervisor.service.runtime.ExperimentFiles;
@Component
public class PythonModelExecutor extends AbstractDockerModelExecutor {
    public PythonModelExecutor(MainPaths paths, ExperimentFiles files) { super(paths,files); }
    @Override public boolean supports(ModelLanguage language) { return language==ModelLanguage.PYTHON; }
}
