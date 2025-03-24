package ru.nsu.fit.evdokimova.database_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.nsu.fit.evdokimova.database_service.dto.StartJsonDto;

import java.util.List;

public interface StartJsonRepository extends JpaRepository<StartJsonDto, Long> {
    List<StartJsonDto> findByExperimentId(Long experimentId);
}
