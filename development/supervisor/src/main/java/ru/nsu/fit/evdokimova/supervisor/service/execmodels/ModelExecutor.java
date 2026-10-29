package ru.nsu.fit.evdokimova.supervisor.service.execmodels;
import com.fasterxml.jackson.databind.JsonNode;
import ru.nsu.fit.evdokimova.supervisor.model.*;
import ru.nsu.fit.evdokimova.supervisor.service.runtime.ModelRunContext;
public interface ModelExecutor {
    boolean supports(ModelLanguage language);
    JsonNode execute(ModelRequest model, ModelRunContext context) throws Exception;
}
