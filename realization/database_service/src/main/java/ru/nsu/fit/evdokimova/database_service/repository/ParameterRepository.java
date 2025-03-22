package ru.nsu.fit.evdokimova.database_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.nsu.fit.evdokimova.database_service.dto.ParameterDto;

import java.util.List;

@Repository
public interface ParameterRepository extends JpaRepository<ParameterDto, Long> {
    List<ParameterDto> findByModelId(Long modelId);
}