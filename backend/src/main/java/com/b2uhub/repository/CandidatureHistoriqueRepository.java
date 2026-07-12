package com.b2uhub.repository;

import com.b2uhub.model.CandidatureHistorique;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CandidatureHistoriqueRepository extends JpaRepository<CandidatureHistorique, Long> {

    List<CandidatureHistorique> findByCandidatureIdOrderByDateChangementDesc(Long candidatureId);
}
