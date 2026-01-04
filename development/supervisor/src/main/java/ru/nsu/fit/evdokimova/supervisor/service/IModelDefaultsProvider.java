package ru.nsu.fit.evdokimova.supervisor.service;

import ru.nsu.fit.evdokimova.supervisor.model.ModelRequest;

import java.util.Map;

public interface IModelDefaultsProvider {
    Map<String, Object> getDefaultParameters (ModelRequest model);
}
