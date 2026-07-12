package com.b2uhub.dto;

import com.b2uhub.model.Candidature;
import com.b2uhub.model.enums.CandidatureStatut;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class CandidatureResponse {

    private Long id;
    private Long missionId;
    private String missionTitre;
    private Long etudiantId;
    private String etudiantNom;
    private CandidatureStatut statut;
    private Double scoreIA;
    private String explicationScore;
    private List<ScoreBreakdownDto> scoreBreakdown;
    private Boolean scoreFromAi;
    private Instant dateCandidature;

    public static CandidatureResponse from(Candidature candidature) {
        return CandidatureResponse.builder()
                .id(candidature.getId())
                .missionId(candidature.getMission().getId())
                .missionTitre(candidature.getMission().getTitre())
                .etudiantId(candidature.getEtudiant().getId())
                .etudiantNom(candidature.getEtudiant().getNom())
                .statut(candidature.getStatut())
                .scoreIA(candidature.getScoreIA())
                .explicationScore(candidature.getExplicationScore())
                .dateCandidature(candidature.getDateCandidature())
                .build();
    }
}
