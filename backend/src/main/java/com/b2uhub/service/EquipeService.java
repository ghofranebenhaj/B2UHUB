package com.b2uhub.service;

import com.b2uhub.dto.EquipeResponse;
import com.b2uhub.model.Candidature;
import com.b2uhub.model.Equipe;
import com.b2uhub.model.Etudiant;
import com.b2uhub.model.Mission;
import com.b2uhub.model.enums.CandidatureStatut;
import com.b2uhub.repository.CandidatureRepository;
import com.b2uhub.repository.EquipeRepository;
import com.b2uhub.repository.EtudiantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EquipeService {

    private final EquipeRepository equipeRepository;
    private final CandidatureRepository candidatureRepository;
    private final EtudiantRepository etudiantRepository;
    private final MissionService missionService;
    private final AiServiceClient aiServiceClient;
    private final NotificationService notificationService;

    public EquipeService(
            EquipeRepository equipeRepository,
            CandidatureRepository candidatureRepository,
            EtudiantRepository etudiantRepository,
            MissionService missionService,
            AiServiceClient aiServiceClient,
            NotificationService notificationService
    ) {
        this.equipeRepository = equipeRepository;
        this.candidatureRepository = candidatureRepository;
        this.etudiantRepository = etudiantRepository;
        this.missionService = missionService;
        this.aiServiceClient = aiServiceClient;
        this.notificationService = notificationService;
    }

    public EquipeResponse formTeam(Long missionId, int tailleEquipe) {
        Mission mission = missionService.getMission(missionId);
        List<Candidature> acceptees = candidatureRepository
                .findByMissionIdAndStatut(missionId, CandidatureStatut.ACCEPTEE);

        if (acceptees.isEmpty()) {
            throw new BusinessException("Aucune candidature acceptée pour former une équipe.");
        }

        List<AiServiceClient.TeamCandidate> candidates = acceptees.stream()
                .map(c -> new AiServiceClient.TeamCandidate(
                        c.getEtudiant().getId(),
                        c.getEtudiant().getNom(),
                        c.getEtudiant().getCompetences(),
                        c.getScoreIA() != null ? c.getScoreIA() : 0
                ))
                .toList();

        AiServiceClient.TeamResult teamResult = aiServiceClient.formTeam(mission, candidates, tailleEquipe);
        List<Long> memberIds = teamResult.memberIds();
        if (memberIds.isEmpty()) {
            memberIds = acceptees.stream()
                    .sorted((a, b) -> Double.compare(
                            b.getScoreIA() != null ? b.getScoreIA() : 0,
                            a.getScoreIA() != null ? a.getScoreIA() : 0))
                    .limit(tailleEquipe)
                    .map(c -> c.getEtudiant().getId())
                    .toList();
            teamResult = new AiServiceClient.TeamResult(
                    memberIds, 0,
                    "Formation locale (top scores) — service IA indisponible.",
                    false
            );
        }

        Equipe equipe = equipeRepository.findByMissionId(missionId).orElse(new Equipe());
        equipe.setMission(mission);
        equipe.setNom("Équipe " + mission.getTitre());
        equipe.getMembres().clear();

        for (Long etudiantId : memberIds) {
            Etudiant etudiant = etudiantRepository.findById(etudiantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Étudiant introuvable: " + etudiantId));
            equipe.getMembres().add(etudiant);
            notificationService.notify(
                    etudiant,
                    "Équipe formée",
                    "Vous avez été sélectionné(e) pour la mission: " + mission.getTitre()
            );
        }

        equipe = equipeRepository.save(equipe);
        mission.setEquipe(equipe);

        return EquipeResponse.builder()
                .id(equipe.getId())
                .nom(equipe.getNom())
                .missionId(mission.getId())
                .missionTitre(mission.getTitre())
                .dateFormation(equipe.getDateFormation())
                .couvertureCompetences(teamResult.coverage())
                .explication(teamResult.explication())
                .membresNoms(equipe.getMembres().stream().map(Etudiant::getNom).toList())
                .build();
    }

    public EquipeResponse getByMission(Long missionId) {
        Equipe equipe = equipeRepository.findByMissionId(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Aucune équipe pour cette mission."));
        return EquipeResponse.builder()
                .id(equipe.getId())
                .nom(equipe.getNom())
                .missionId(equipe.getMission().getId())
                .missionTitre(equipe.getMission().getTitre())
                .dateFormation(equipe.getDateFormation())
                .membresNoms(equipe.getMembres().stream().map(Etudiant::getNom).toList())
                .build();
    }
}
