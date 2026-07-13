package com.b2uhub.service;

import com.b2uhub.dto.AiMatchResponse;
import com.b2uhub.model.Etudiant;
import com.b2uhub.model.Mission;
import com.b2uhub.repository.EtudiantRepository;
import com.b2uhub.repository.MissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service dédié au matching étudiant / mission (score de similarité IA + fallback local).
 */
@Service
@Transactional(readOnly = true)
public class MatchingService {

    private static final Logger log = LoggerFactory.getLogger(MatchingService.class);

    private final AiServiceClient aiServiceClient;
    private final EtudiantRepository etudiantRepository;
    private final MissionRepository missionRepository;

    public MatchingService(
            AiServiceClient aiServiceClient,
            EtudiantRepository etudiantRepository,
            MissionRepository missionRepository
    ) {
        this.aiServiceClient = aiServiceClient;
        this.etudiantRepository = etudiantRepository;
        this.missionRepository = missionRepository;
    }

    public AiMatchResponse match(Long etudiantId, Long missionId) {
        Etudiant etudiant = etudiantRepository.findById(etudiantId)
                .orElseThrow(() -> new ResourceNotFoundException("Étudiant introuvable: " + etudiantId));
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission introuvable: " + missionId));

        AiMatchResponse response = aiServiceClient.match(etudiant, mission);
        log.debug("Matching étudiant={} mission={} similarity={} methode={}",
                etudiantId, missionId, response.getSimilarity(), response.getMethode());
        return response;
    }

    public double computeLocalSimilarity(Etudiant etudiant, Mission mission) {
        return aiServiceClient.matchingSimilarity(
                etudiant.getCompetences(),
                mission.getCompetencesRequises()
        ) * 100;
    }
}
