package com.b2uhub.service;

import com.b2uhub.dto.*;
import com.b2uhub.model.Candidature;
import com.b2uhub.model.Etudiant;
import com.b2uhub.model.Mission;
import com.b2uhub.model.enums.CandidatureStatut;
import com.b2uhub.model.enums.MissionStatut;
import com.b2uhub.repository.CandidatureRepository;
import com.b2uhub.repository.EntrepriseRepository;
import com.b2uhub.repository.EtudiantRepository;
import com.b2uhub.repository.MissionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AiHubService {

    private final AiServiceClient aiServiceClient;
    private final EtudiantRepository etudiantRepository;
    private final MissionRepository missionRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final CandidatureRepository candidatureRepository;
    private final String aiServiceUrl;

    public AiHubService(
            AiServiceClient aiServiceClient,
            EtudiantRepository etudiantRepository,
            MissionRepository missionRepository,
            EntrepriseRepository entrepriseRepository,
            CandidatureRepository candidatureRepository,
            @Value("${b2u.ai-service.base-url}") String aiServiceUrl
    ) {
        this.aiServiceClient = aiServiceClient;
        this.etudiantRepository = etudiantRepository;
        this.missionRepository = missionRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.candidatureRepository = candidatureRepository;
        this.aiServiceUrl = aiServiceUrl;
    }

    public Map<String, Object> status() {
        return Map.of(
                "aiServiceUp", aiServiceClient.isAvailable(),
                "aiServiceUrl", aiServiceUrl,
                "agents", List.of(
                        "SmartMatchingAgent",
                        "ScoringAgent",
                        "CollaborationInnovationAgent"
                )
        );
    }

    public List<RecommendItemDto> recommendForEtudiant(Long etudiantId, int topK) {
        Etudiant etudiant = getEtudiant(etudiantId);
        List<Mission> ouvertes = missionRepository.findByStatut(MissionStatut.OUVERTE);
        return aiServiceClient.recommend(etudiant, ouvertes, topK);
    }

    public AiMatchResponse match(Long etudiantId, Long missionId) {
        Etudiant etudiant = getEtudiant(etudiantId);
        Mission mission = getMission(missionId);
        return aiServiceClient.match(etudiant, mission);
    }

    public AiServiceClient.CvAnalysisResult analyzeCv(Long etudiantId) {
        return aiServiceClient.analyzeCv(getEtudiant(etudiantId));
    }

    public SmartMatchingResponseDto runSmartMatchingAgent(Long etudiantId, Long missionId) {
        Etudiant etudiant = getEtudiant(etudiantId);
        List<Mission> missions = missionRepository.findByStatut(MissionStatut.OUVERTE);

        List<Map<String, Object>> missionMaps = missions.stream().map(this::missionToMap).toList();
        List<Map<String, Object>> candidats = List.of();
        if (missionId != null) {
            candidats = candidatureRepository.findByMissionIdAndStatut(missionId, CandidatureStatut.ACCEPTEE)
                    .stream()
                    .map(c -> Map.<String, Object>of(
                            "etudiant_id", c.getEtudiant().getId(),
                            "nom", c.getEtudiant().getNom(),
                            "competences", c.getEtudiant().getCompetences(),
                            "score_ia", c.getScoreIA() != null ? c.getScoreIA() : 0
                    ))
                    .toList();
        }

        Map<String, Object> body = new HashMap<>();
        body.put("etudiant_nom", etudiant.getNom());
        body.put("etudiant_competences", etudiant.getCompetences());
        body.put("missions", missionMaps);
        body.put("mission_cible_id", missionId);
        body.put("candidats_equipe", candidats);
        body.put("taille_equipe", 2);

        return aiServiceClient.smartMatching(body);
    }

    public CollaborationResponseDto runCollaborationAgent() {
        List<Map<String, Object>> entreprises = entrepriseRepository.findAll().stream()
                .map(e -> Map.<String, Object>of("nom", e.getNom(), "secteur", e.getSecteur() != null ? e.getSecteur() : ""))
                .toList();

        List<Map<String, Object>> etudiants = etudiantRepository.findAll().stream()
                .map(e -> Map.<String, Object>of(
                        "nom", e.getNom(),
                        "filiere", e.getFiliere() != null ? e.getFiliere() : "",
                        "competences", e.getCompetences()
                ))
                .toList();

        List<Map<String, Object>> missions = missionRepository.findAll().stream()
                .map(m -> {
                    Map<String, Object> map = new HashMap<>(missionToMap(m));
                    map.put("statut", m.getStatut().name());
                    return map;
                })
                .toList();

        List<Map<String, Object>> laboratoires = List.of(
                Map.of("nom", "Lab IA & Data — ENSIAS", "domaines", List.of("Machine Learning", "NLP", "Python", "FastAPI")),
                Map.of("nom", "Lab Software Engineering", "domaines", List.of("Java", "Angular", "DevOps", "Spring Boot"))
        );

        Map<String, Object> body = Map.of(
                "entreprises", entreprises,
                "laboratoires", laboratoires,
                "etudiants", etudiants,
                "missions", missions,
                "max_opportunites", 6
        );

        return aiServiceClient.collaboration(body);
    }

    public double resolvePerformance(Etudiant etudiant) {
        if (etudiant.getPerformanceAnterieure() != null) {
            return etudiant.getPerformanceAnterieure();
        }
        List<Candidature> history = candidatureRepository.findByEtudiantId(etudiant.getId());
        if (history.isEmpty()) return 50.0;
        long accepted = history.stream().filter(c -> c.getStatut() == CandidatureStatut.ACCEPTEE).count();
        double rate = (double) accepted / history.size() * 100;
        double avgScore = history.stream()
                .filter(c -> c.getScoreIA() != null)
                .mapToDouble(Candidature::getScoreIA)
                .average()
                .orElse(50.0);
        return (rate + avgScore) / 2;
    }

    private Map<String, Object> missionToMap(Mission m) {
        return Map.of(
                "id", m.getId(),
                "titre", m.getTitre(),
                "entreprise", m.getEntreprise().getNom(),
                "competences_requises", m.getCompetencesRequises()
        );
    }

    private Etudiant getEtudiant(Long id) {
        return etudiantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Étudiant introuvable: " + id));
    }

    private Mission getMission(Long id) {
        return missionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mission introuvable: " + id));
    }
}
