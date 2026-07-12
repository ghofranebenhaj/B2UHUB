package com.b2uhub.repository;

import com.b2uhub.model.Equipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EquipeRepository extends JpaRepository<Equipe, Long> {

    Optional<Equipe> findByMissionId(Long missionId);
}
