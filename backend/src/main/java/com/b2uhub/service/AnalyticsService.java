package com.b2uhub.service;

import com.b2uhub.dto.AnalyticsSummaryResponse;
import com.b2uhub.model.Candidature;
import com.b2uhub.model.enums.CandidatureStatut;
import com.b2uhub.model.enums.MissionStatut;
import com.b2uhub.repository.CandidatureRepository;
import com.b2uhub.repository.MissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final MissionRepository missionRepository;
    private final CandidatureRepository candidatureRepository;

    public AnalyticsService(MissionRepository missionRepository, CandidatureRepository candidatureRepository) {
        this.missionRepository = missionRepository;
        this.candidatureRepository = candidatureRepository;
    }

    public AnalyticsSummaryResponse getSummary() {
        long ouvertes = missionRepository.findByStatut(MissionStatut.OUVERTE).size();
        long enCours = missionRepository.findByStatut(MissionStatut.EN_COURS).size();
        long enAttente = candidatureRepository.findAll().stream()
                .filter(c -> c.getStatut() == CandidatureStatut.EN_ATTENTE)
                .count();
        long acceptees = candidatureRepository.findAll().stream()
                .filter(c -> c.getStatut() == CandidatureStatut.ACCEPTEE)
                .count();
        double scoreMoyen = candidatureRepository.findAll().stream()
                .filter(c -> c.getScoreIA() != null)
                .mapToDouble(Candidature::getScoreIA)
                .average()
                .orElse(0.0);

        return AnalyticsSummaryResponse.builder()
                .missionsOuvertes(ouvertes)
                .missionsEnCours(enCours)
                .candidaturesEnAttente(enAttente)
                .candidaturesAcceptees(acceptees)
                .scoreMoyen(scoreMoyen)
                .build();
    }
}
