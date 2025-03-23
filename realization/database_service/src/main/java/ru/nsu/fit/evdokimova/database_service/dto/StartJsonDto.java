package ru.nsu.fit.evdokimova.database_service.dto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Entity
@Table(name = "start_json")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StartJsonDto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long experimentId;

    @Column(nullable = false)
    private String modelId;

    @ElementCollection
    @CollectionTable(name = "start_json_parameters", joinColumns = @JoinColumn(name = "start_json_id"))
    @MapKeyColumn(name = "parameter_name")
    @Column(name = "parameter_value")
    private Map<String, String> parameters;


    public StartJsonDto(Long experimentId, String modelId, Map<String, String> parameters) {
        this.experimentId = experimentId;
        this.modelId = modelId;
        this.parameters = parameters;
    }
}