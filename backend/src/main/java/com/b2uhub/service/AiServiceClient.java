package com.b2uhub.service;

import com.b2uhub.dto.*;
import com.b2uhub.model.Etudiant;
import com.b2uhub.model.Mission;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Façade métier au-dessus du client OpenFeign AiServiceFeignClient.
 * Conserve exactement la même logique de scoring/fallback qu'avant,
 * seul le transport HTTP a changé (RestClient -> Feign).
 */
@Component
public class AiServiceClient {

    private final AiServiceFeignClient feignClient;
    private final ObjectMapper objectMapper;

    public AiServiceClient(AiServiceFeignClient feignClient, ObjectMapper objectMapper) {
        this.feignClient = feignClient;
        this.objectMapper = objectMapper;
    }

    public boolean isAvailable() {
        try {
            Map<String, Object> status = feignClient.health();
            return status != null && "UP".equals(status.get("status"));
        } catch (Exception e) {
            return false;
        }
    }

    public ScoreResult scoreCandidature(Etudiant etudiant, Mission mission, double performanceAnterieure) {
        Map<String, Object> body = scorePayload(etudiant, mission, performanceAnterieure);
        try {
            Map<String, Object> response = feignClient.score(body);
            if (response != null && response.get("score") instanceof Number scoreNum) {
                String explication = (String) response.get("explication");
                List<ScoreBreakdownDto> breakdown = castBreakdown(response.get("breakdown"));
                return new ScoreResult(scoreNum.doubleValue(), explication, breakdown, true);
            }
        } catch (Exception ignored) {
        }
        return computeLocalScore(etudiant, mission, performanceAnterieure);
    }

    public SmartMatchingResponseDto smartMatching(Map<String, Object> body) {
        try {
            Map<String, Object> response = feignClient.smartMatching(body);
            if (response != null) return mapSmartMatching(response);
        } catch (Exception ignored) {
        }
        SmartMatchingResponseDto fallback = new SmartMatchingResponseDto();
        fallback.setAgent("SmartMatchingAgent");
        fallback.setMessage("Service IA hors ligne — démarrez uvicorn sur le port 8000.");
        fallback.setMethode("fallback");
        fallback.setProfilsRecommandes(List.of());
        return fallback;
    }

    public CollaborationResponseDto collaboration(Map<String, Object> body) {
        try {
            Map<String, Object> response = feignClient.collaboration(body);
            if (response != null) return mapCollaboration(response);
        } catch (Exception ignored) {
        }
        CollaborationResponseDto fallback = new CollaborationResponseDto();
        fallback.setAgent("CollaborationInnovationAgent");
        fallback.setMessage("Service IA hors ligne.");
        fallback.setLlmActive(false);
        fallback.setMethode("fallback");
        fallback.setOpportunites(List.of());
        return fallback;
    }

    public AiMatchResponse match(Etudiant etudiant, Mission mission) {
        Map<String, Object> body = Map.of(
                "etudiant_competences", etudiant.getCompetences(),
                "mission_competences", mission.getCompetencesRequises()
        );
        try {
            Map<String, Object> res = feignClient.match(body);
            if (res != null) {
                AiMatchResponse response = new AiMatchResponse();
                if (res.get("similarity") instanceof Number n) response.setSimilarity(n.doubleValue());
                response.setMethode((String) res.get("methode"));
                response.setCompetencesCommunes(castStringList(res.get("competences_communes")));
                return response;
            }
        } catch (Exception ignored) {
        }
        double sim = matchingSimilarity(etudiant.getCompetences(), mission.getCompetencesRequises()) * 100;
        AiMatchResponse fallback = new AiMatchResponse();
        fallback.setSimilarity(sim);
        fallback.setMethode("local_fallback");
        fallback.setCompetencesCommunes(List.of());
        return fallback;
    }

    public List<RecommendItemDto> recommend(Etudiant etudiant, List<Mission> missions, int topK) {
        List<Map<String, Object>> missionItems = missions.stream()
                .map(m -> Map.<String, Object>of(
                        "id", m.getId(),
                        "titre", m.getTitre(),
                        "entreprise", m.getEntreprise().getNom(),
                        "competences_requises", m.getCompetencesRequises()
                ))
                .toList();

        Map<String, Object> body = Map.of(
                "etudiant_competences", etudiant.getCompetences(),
                "missions", missionItems,
                "top_k", topK
        );
        try {
            Map<String, Object> response = feignClient.recommend(body);
            if (response != null && response.get("recommandations") instanceof List<?> list) {
                return list.stream().map(this::mapRecommend).toList();
            }
        } catch (Exception ignored) {
        }
        return List.of();
    }

    public TeamResult formTeam(Mission mission, List<TeamCandidate> candidats, int taille) {
        List<Map<String, Object>> items = candidats.stream()
                .map(c -> Map.<String, Object>of(
                        "etudiant_id", c.etudiantId(),
                        "nom", c.nom(),
                        "competences", c.competences(),
                        "score_ia", c.scoreIa()
                ))
                .toList();

        Map<String, Object> body = Map.of(
                "mission_competences", mission.getCompetencesRequises(),
                "candidats", items,
                "taille_equipe", taille
        );
        try {
            Map<String, Object> response = feignClient.team(body);
            if (response != null) {
                double coverage = response.get("couverture_competences") instanceof Number n ? n.doubleValue() : 0;
                String explication = (String) response.get("explication");
                List<Long> ids = List.of();
                if (response.get("membres") instanceof List<?> membres) {
                    ids = membres.stream()
                            .filter(m -> m instanceof Map<?, ?> map && map.get("etudiant_id") instanceof Number num)
                            .map(m -> ((Number) ((Map<?, ?>) m).get("etudiant_id")).longValue())
                            .toList();
                }
                return new TeamResult(ids, coverage, explication, true);
            }
        } catch (Exception ignored) {
        }
        return new TeamResult(List.of(), 0, "Service IA indisponible — formation locale impossible.", false);
    }

    public CvAnalysisResult analyzeCv(Etudiant etudiant) {
        Map<String, Object> body = Map.of(
                "cv_texte", etudiant.getCvTexte() != null ? etudiant.getCvTexte() : "",
                "competences_connues", etudiant.getCompetences()
        );
        try {
            Map<String, Object> response = feignClient.analyzeCv(body);
            if (response != null) {
                return new CvAnalysisResult(
                        castStringList(response.get("competences_detectees")),
                        castStringList(response.get("mots_cles")),
                        response.get("score_pertinence") instanceof Number n ? n.doubleValue() : 0,
                        true
                );
            }
        } catch (Exception ignored) {
        }
        return new CvAnalysisResult(etudiant.getCompetences(), List.of(), 50.0, false);
    }

    private Map<String, Object> scorePayload(Etudiant etudiant, Mission mission, double performance) {
        Map<String, Object> body = new HashMap<>();
        body.put("etudiant_competences", etudiant.getCompetences());
        body.put("mission_competences", mission.getCompetencesRequises());
        body.put("annees_experience", etudiant.getAnneesExperience());
        body.put("nb_projets_passes", etudiant.getNombreProjetsRealises() != null ? etudiant.getNombreProjetsRealises() : 0);
        body.put("performance_anterieure", performance);
        body.put("soft_skills", etudiant.getScoreSoftSkills());
        body.put("disponibilite", etudiant.getDisponibiliteHeuresSemaine());
        body.put("cv_texte", etudiant.getCvTexte());
        return body;
    }

    private SmartMatchingResponseDto mapSmartMatching(Map<String, Object> r) {
        SmartMatchingResponseDto dto = new SmartMatchingResponseDto();
        dto.setAgent((String) r.get("agent"));
        dto.setMessage((String) r.get("message"));
        dto.setMethode((String) r.get("methode"));
        if (r.get("profils_recommandes") instanceof List<?> list) {
            dto.setProfilsRecommandes(list.stream().map(item -> {
                Map<?, ?> m = (Map<?, ?>) item;
                ProfileMatchDto p = new ProfileMatchDto();
                if (m.get("mission_id") instanceof Number n) p.setMissionId(n.longValue());
                p.setMissionTitre((String) m.get("mission_titre"));
                p.setEntreprise((String) m.get("entreprise"));
                if (m.get("score_matching") instanceof Number s) p.setScoreMatching(s.doubleValue());
                p.setCompetencesAlignees(castStringList(m.get("competences_alignees")));
                p.setRecommandation((String) m.get("recommandation"));
                p.setJustification((String) m.get("justification"));
                return p;
            }).toList());
        }
        if (r.get("suggestion_equipe") instanceof Map<?, ?> team) {
            TeamSuggestionDto t = new TeamSuggestionDto();
            if (team.get("mission_id") instanceof Number n) t.setMissionId(n.longValue());
            t.setMembres(castStringList(team.get("membres")));
            if (team.get("couverture") instanceof Number c) t.setCouverture(c.doubleValue());
            t.setExplication((String) team.get("explication"));
            dto.setSuggestionEquipe(t);
        }
        return dto;
    }

    private CollaborationResponseDto mapCollaboration(Map<String, Object> r) {
        CollaborationResponseDto dto = new CollaborationResponseDto();
        dto.setAgent((String) r.get("agent"));
        dto.setMessage((String) r.get("message"));
        dto.setMethode((String) r.get("methode"));
        dto.setLlmActive(Boolean.TRUE.equals(r.get("llm_active")));
        if (r.get("opportunites") instanceof List<?> list) {
            dto.setOpportunites(list.stream().map(item -> {
                Map<?, ?> m = (Map<?, ?>) item;
                CollaborationOpportunityDto o = new CollaborationOpportunityDto();
                o.setType((String) m.get("type"));
                o.setTitre((String) m.get("titre"));
                o.setActeurs(castStringList(m.get("acteurs")));
                o.setThematique((String) m.get("thematique"));
                if (m.get("score_potentiel") instanceof Number s) o.setScorePotentiel(s.doubleValue());
                o.setDescription((String) m.get("description"));
                o.setActionSuggeree((String) m.get("action_suggeree"));
                return o;
            }).toList());
        }
        return dto;
    }

    private RecommendItemDto mapRecommend(Object item) {
        Map<?, ?> map = (Map<?, ?>) item;
        RecommendItemDto dto = new RecommendItemDto();
        if (map.get("mission_id") instanceof Number n) dto.setMissionId(n.longValue());
        dto.setTitre((String) map.get("titre"));
        if (map.get("score_matching") instanceof Number s) dto.setScoreMatching(s.doubleValue());
        dto.setRaison((String) map.get("raison"));
        return dto;
    }

    @SuppressWarnings("unchecked")
    private List<String> castStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    private List<ScoreBreakdownDto> castBreakdown(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(item -> objectMapper.convertValue(item, ScoreBreakdownDto.class))
                    .toList();
        }
        return List.of();
    }

    public double matchingSimilarity(List<String> etudiantCompetences, List<String> missionCompetences) {
        if (etudiantCompetences == null || missionCompetences == null || missionCompetences.isEmpty()) {
            return 0.0;
        }
        long matches = etudiantCompetences.stream()
                .map(String::toLowerCase)
                .filter(c -> missionCompetences.stream().map(String::toLowerCase).anyMatch(c::equals))
                .count();
        return (double) matches / missionCompetences.size();
    }

    private ScoreResult computeLocalScore(Etudiant etudiant, Mission mission, double performance) {
        double competences = matchingSimilarity(etudiant.getCompetences(), mission.getCompetencesRequises()) * 100;
        int projets = etudiant.getNombreProjetsRealises() != null ? etudiant.getNombreProjetsRealises() : 0;
        double projetsNote = Math.min(100, projets * 20.0);
        double softSkills = etudiant.getScoreSoftSkills() != null ? etudiant.getScoreSoftSkills() : 50;

        double score = competences * 0.35 + projetsNote * 0.25 + softSkills * 0.20 + performance * 0.20;
        String explication = String.format(
                "Compétences: %.1f (35%%) | Projets: %.1f (25%%) | Soft skills: %.1f (20%%) | Performance: %.1f (20%%)",
                competences, projetsNote, softSkills, performance
        );
        return new ScoreResult(score, explication, List.of(), false);
    }

    public record ScoreResult(double score, String explication, List<ScoreBreakdownDto> breakdown, boolean fromAi) {}
    public record TeamCandidate(Long etudiantId, String nom, List<String> competences, double scoreIa) {}
    public record TeamResult(List<Long> memberIds, double coverage, String explication, boolean fromAi) {}
    public record CvAnalysisResult(List<String> competences, List<String> keywords, double score, boolean fromAi) {}
}
