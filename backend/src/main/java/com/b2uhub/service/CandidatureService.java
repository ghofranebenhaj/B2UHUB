package com.b2uhub.service;

import com.b2uhub.dto.CandidatureHistoriqueResponse;
import com.b2uhub.dto.CandidatureRequest;
import com.b2uhub.dto.CandidatureResponse;
import com.b2uhub.model.Candidature;
import com.b2uhub.model.CandidatureHistorique;
import com.b2uhub.model.Etudiant;
import com.b2uhub.model.Mission;
import com.b2uhub.model.enums.CandidatureStatut;
import com.b2uhub.repository.CandidatureHistoriqueRepository;
import com.b2uhub.repository.CandidatureRepository;
import com.b2uhub.repository.EtudiantRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class CandidatureService {

    private final CandidatureRepository candidatureRepository;
    private final CandidatureHistoriqueRepository historiqueRepository;
    private final EtudiantRepository etudiantRepository;
    private final MissionService missionService;
    private final AiServiceClient aiServiceClient;
    private final AiHubService aiHubService;
    private final NotificationService notificationService;
    private final double preselectionScoreThreshold;
    private final int maxCandidaturesActives;

    private static final List<CandidatureStatut> STATUTS_ACTIFS = List.of(
            CandidatureStatut.EN_ATTENTE,
            CandidatureStatut.PRESELECTIONNEE,
            CandidatureStatut.ENTRETIEN
    );

    public CandidatureService(
            CandidatureRepository candidatureRepository,
            CandidatureHistoriqueRepository historiqueRepository,
            EtudiantRepository etudiantRepository,
            MissionService missionService,
            AiServiceClient aiServiceClient,
            AiHubService aiHubService,
            NotificationService notificationService,
            @Value("${b2u.candidature.preselection-score-threshold:70}") double preselectionScoreThreshold,
            @Value("${b2u.candidature.max-candidatures-actives:5}") int maxCandidaturesActives
    ) {
        this.candidatureRepository = candidatureRepository;
        this.historiqueRepository = historiqueRepository;
        this.etudiantRepository = etudiantRepository;
        this.missionService = missionService;
        this.aiServiceClient = aiServiceClient;
        this.aiHubService = aiHubService;
        this.notificationService = notificationService;
        this.preselectionScoreThreshold = preselectionScoreThreshold;
        this.maxCandidaturesActives = maxCandidaturesActives;
    }

    public List<CandidatureResponse> findByMission(Long missionId) {
        return candidatureRepository.findByMissionId(missionId).stream()
                .sorted(Comparator
                        .comparing(Candidature::getScoreIA, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Candidature::getDateCandidature, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(CandidatureResponse::from)
                .toList();
    }

    public long countByMission(Long missionId) {
        missionService.getMission(missionId);
        return candidatureRepository.countByMissionId(missionId);
    }

    public CandidatureResponse postuler(CandidatureRequest request) {
        if (candidatureRepository.existsByMissionIdAndEtudiantId(request.getMissionId(), request.getEtudiantId())) {
            throw new BusinessException("Candidature déjà existante pour cette mission");
        }

        long candidaturesActives = candidatureRepository.countByEtudiantIdAndStatutIn(
                request.getEtudiantId(), STATUTS_ACTIFS
        );
        if (candidaturesActives >= maxCandidaturesActives) {
            throw new BusinessException(
                    "Limite de " + maxCandidaturesActives + " candidatures actives atteinte. " +
                    "Attendez une réponse avant de postuler à nouveau."
            );
        }

        Mission mission = missionService.getMission(request.getMissionId());
        Etudiant etudiant = etudiantRepository.findById(request.getEtudiantId())
                .orElseThrow(() -> new ResourceNotFoundException("Étudiant introuvable: " + request.getEtudiantId()));

        double performance = aiHubService.resolvePerformance(etudiant);
        AiServiceClient.ScoreResult score = aiServiceClient.scoreCandidature(etudiant, mission, performance);

        Candidature candidature = new Candidature();
        candidature.setMission(mission);
        candidature.setEtudiant(etudiant);
        candidature.setScoreIA(score.score());
        candidature.setExplicationScore(score.explication());
        candidature.setStatut(CandidatureStatut.EN_ATTENTE);

        Candidature saved = candidatureRepository.save(candidature);

        if (score.score() >= preselectionScoreThreshold) {
            changerStatut(saved, CandidatureStatut.PRESELECTIONNEE, false);
            saved = candidatureRepository.save(saved);
        }

        notificationService.notify(
                etudiant,
                "Candidature envoyée",
                buildPostulerMessage(mission, saved, score.score())
        );
        if (mission.getEntreprise() != null) {
            notificationService.notify(
                    mission.getEntreprise(),
                    "Nouvelle candidature",
                    etudiant.getNom() + " a postulé à « " + mission.getTitre() + " »"
            );
        }

        return toResponse(saved, score);
    }

    public CandidatureResponse updateStatut(Long id, CandidatureStatut statut) {
        Candidature candidature = getCandidature(id);
        changerStatut(candidature, statut, true);
        candidatureRepository.save(candidature);

        notificationService.notify(
                candidature.getEtudiant(),
                notificationTitle(statut),
                "Mission « " + candidature.getMission().getTitre() + " » — statut: " + statut
        );

        return CandidatureResponse.from(candidature);
    }

    @Transactional(readOnly = true)
    public List<CandidatureHistoriqueResponse> getHistorique(Long candidatureId) {
        getCandidature(candidatureId);
        return historiqueRepository.findByCandidatureIdOrderByDateChangementDesc(candidatureId).stream()
                .map(CandidatureHistoriqueResponse::from)
                .toList();
    }

    private Candidature getCandidature(Long id) {
        return candidatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable: " + id));
    }

    private void changerStatut(Candidature candidature, CandidatureStatut nouveauStatut, boolean notifyOnTerminal) {
        CandidatureStatut ancienStatut = candidature.getStatut();
        if (ancienStatut == nouveauStatut) {
            return;
        }
        if (ancienStatut == CandidatureStatut.ACCEPTEE || ancienStatut == CandidatureStatut.REFUSEE) {
            throw new BusinessException("Impossible de modifier une candidature " + ancienStatut);
        }

        candidature.setStatut(nouveauStatut);
        enregistrerHistorique(candidature, ancienStatut, nouveauStatut);

        if (notifyOnTerminal && (nouveauStatut == CandidatureStatut.PRESELECTIONNEE)) {
            notificationService.notify(
                    candidature.getEtudiant(),
                    "Candidature présélectionnée",
                    "Votre candidature à « " + candidature.getMission().getTitre() + " » a été présélectionnée."
            );
        }
    }

    private void enregistrerHistorique(Candidature candidature, CandidatureStatut ancienStatut, CandidatureStatut nouveauStatut) {
        CandidatureHistorique historique = new CandidatureHistorique();
        historique.setCandidature(candidature);
        historique.setAncienStatut(ancienStatut);
        historique.setNouveauStatut(nouveauStatut);
        historiqueRepository.save(historique);
    }

    private String buildPostulerMessage(Mission mission, Candidature candidature, double score) {
        String base = "Votre candidature à « " + mission.getTitre() + " » a été enregistrée. Score IA: " + Math.round(score);
        if (candidature.getStatut() == CandidatureStatut.PRESELECTIONNEE) {
            return base + ". Vous êtes automatiquement présélectionné(e).";
        }
        return base;
    }

    private String notificationTitle(CandidatureStatut statut) {
        return switch (statut) {
            case ACCEPTEE -> "Candidature acceptée";
            case REFUSEE -> "Candidature refusée";
            case PRESELECTIONNEE -> "Candidature présélectionnée";
            case ENTRETIEN -> "Convocation à un entretien";
            default -> "Candidature mise à jour";
        };
    }

    private CandidatureResponse toResponse(Candidature c, AiServiceClient.ScoreResult score) {
        return CandidatureResponse.builder()
                .id(c.getId())
                .missionId(c.getMission().getId())
                .missionTitre(c.getMission().getTitre())
                .etudiantId(c.getEtudiant().getId())
                .etudiantNom(c.getEtudiant().getNom())
                .statut(c.getStatut())
                .scoreIA(c.getScoreIA())
                .explicationScore(c.getExplicationScore())
                .scoreBreakdown(score.breakdown())
                .scoreFromAi(score.fromAi())
                .dateCandidature(c.getDateCandidature())
                .build();
    }
}
