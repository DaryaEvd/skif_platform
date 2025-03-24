package ru.nsu.fit.evdokimova.database_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.nsu.fit.evdokimova.database_service.dto.ModelDto;

import java.util.List;

@Repository
public interface ModelRepository extends JpaRepository<ModelDto, Long> {
    List<ModelDto> findByExperimentIdOrderByExecutionOrderAsc(Long experimentId);
}