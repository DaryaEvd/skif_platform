package ru.nsu.fit.evdokimova.database_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.nsu.fit.evdokimova.database_service.dto.*;
import ru.nsu.fit.evdokimova.database_service.repository.ExperimentRepository;
import ru.nsu.fit.evdokimova.database_service.repository.ModelRepository;
import ru.nsu.fit.evdokimova.database_service.repository.ParameterRepository;
import ru.nsu.fit.evdokimova.database_service.repository.StartJsonRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExperimentService {

    private final ExperimentRepository experimentRepository;
    private final ModelRepository modelRepository;
    private final ParameterRepository parameterRepository;
    private final StartJsonRepository startJsonRepository;

    public void processExperiment(RequestExperimentFromClient request) {
//        List<ModelDto> models = modelRepository.findByExperimentIdOrderByExecutionOrderAsc(Long.parseLong(request.getExperimentId()));
        List<ModelDto> models = modelRepository.findByExperimentIdOrderByExecutionOrderAsc(request.getExperimentId());

        for (ModelDto model : models) {
            List<ParameterDto> parameters = parameterRepository.findByModelId(model.getId());
            Map<String, String> paramsMap = parameters.stream()
                    .collect(Collectors.toMap(ParameterDto::getName, ParameterDto::getValue));

            StartJsonDto startJson = new StartJsonDto(request.getExperimentId(), model.getId().toString(), paramsMap);
            startJsonRepository.save(startJson);
        }
    }

//    public List<StartJsonDto> generateStartFiles(Long experimentId) {
//        ExperimentDto experiment = experimentRepository.findById(experimentId)
//                .orElseThrow(() -> new IllegalArgumentException("Experiment not found"));
//
//        List<ModelDto> models = modelRepository.findByExperimentIdOrderByExecutionOrderAsc(experiment.getId());
//
//        List<StartJsonDto> startFiles = new ArrayList<>();
//        for (ModelDto model : models) {
//            List<ParameterDto> parameters = parameterRepository.findByModelId(model.getId());
//            Map<String, String> paramsMap = parameters.stream()
//                    .collect(Collectors.toMap(ParameterDto::getName, ParameterDto::getValue));
//
//            startFiles.add(new StartJsonDto(experiment.getId(), model.getId().toString(), paramsMap));
//        }
//
//        return startFiles;
//    }
}
