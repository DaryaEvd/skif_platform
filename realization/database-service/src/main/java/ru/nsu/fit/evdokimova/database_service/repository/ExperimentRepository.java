package ru.nsu.fit.evdokimova.database_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.nsu.fit.evdokimova.database_service.dto.ExperimentDto;

@Repository
public interface ExperimentRepository extends JpaRepository<ExperimentDto, Long> {
}