package ru.nsu.fit.evdokimova.supervisor.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.nsu.fit.evdokimova.supervisor.service.ExperimentService;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SupervisorController.class)
class SupervisorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExperimentService experimentService;

    @Test
    void startExperimentGeneratesIdProcessesExperimentAndReturnsSuccessResponse() throws Exception {
        mockMvc.perform(post("/api/experiments/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "experimentName": "calculations",
                                  "models": [
                                    {
                                      "modelId": "1",
                                      "name": "shtorka_model",
                                      "order": 1,
                                      "version": "1.0.0",
                                      "language": "PYTHON",
                                      "modelPath": "model_01",
                                      "parameters": {}
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.experimentId").isNumber())
                .andExpect(jsonPath("$.message").value("Experiment started successfully"))
                .andExpect(jsonPath("$.modelsCount").value(1))
                .andExpect(jsonPath("$.startTime").exists());

        verify(experimentService).processExperiment(any());
    }

    @Test
    void startExperimentReturnsInternalServerErrorWhenServiceFails() throws Exception {
        doThrow(new IllegalStateException("executor failed"))
                .when(experimentService)
                .processExperiment(any());

        mockMvc.perform(post("/api/experiments/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "experimentName": "calculations",
                                  "models": []
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.experimentId").doesNotExist())
                .andExpect(jsonPath("$.message").value(startsWith("Error: executor failed")))
                .andExpect(jsonPath("$.modelsCount").value(0))
                .andExpect(jsonPath("$.startTime").exists());
    }
}
