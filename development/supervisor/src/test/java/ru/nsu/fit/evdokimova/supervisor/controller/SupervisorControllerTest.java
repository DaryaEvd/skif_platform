package ru.nsu.fit.evdokimova.supervisor.controller;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.nsu.fit.evdokimova.supervisor.service.ExperimentService;
import ru.nsu.fit.evdokimova.supervisor.service.runtime.ExperimentFiles;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SupervisorController.class)
class SupervisorControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ExperimentService service;
    @MockitoBean ExperimentFiles files;
    @Test void originalStartRouteReturnsAcceptedAndSameResponseFields() throws Exception {
        doAnswer(call->{call.<ru.nsu.fit.evdokimova.supervisor.model.RequestExperimentFromClient>getArgument(0).generateId();return null;}).when(service).processExperiment(any());
        mvc.perform(post("/api/experiments/start").contentType("application/json").content("{\"experimentName\":\"test\",\"models\":[]}"))
            .andExpect(status().isAccepted()).andExpect(jsonPath("$.experimentId").isNumber())
            .andExpect(jsonPath("$.modelsCount").value(0)).andExpect(jsonPath("$.startTime").exists());
    }
    @Test void badConfigurationIs400() throws Exception {
        doThrow(new IllegalArgumentException("conflicting energy")).when(service).processExperiment(any());
        mvc.perform(post("/api/experiments/start").contentType("application/json").content("{\"models\":[]}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("conflicting energy"));
    }
    @Test void existingNamespaceExposesStatusAndCancel() throws Exception {
        var state=new ru.nsu.fit.evdokimova.supervisor.model.ExperimentStatus();state.setExperimentId(1L);state.setStatus("RUNNING");
        when(service.status(1)).thenReturn(state);
        mvc.perform(get("/api/experiments/1/status")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RUNNING"));
        state.setStatus("CANCELLING");when(service.cancel(1)).thenReturn(state);
        mvc.perform(post("/api/experiments/1/cancel")).andExpect(status().isAccepted());
    }
}
