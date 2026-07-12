package com.b2uhub.dto;

import com.b2uhub.model.CandidatureHistorique;
import com.b2uhub.model.enums.CandidatureStatut;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class CandidatureHistoriqueResponse {

    private Long id;
    private Long candidatureId;
    private CandidatureStatut ancienStatut;
    private CandidatureStatut nouveauStatut;
    private Instant dateChangement;

    public static CandidatureHistoriqueResponse from(CandidatureHistorique historique) {
        return CandidatureHistoriqueResponse.builder()
                .id(historique.getId())
                .candidatureId(historique.getCandidature().getId())
                .ancienStatut(historique.getAncienStatut())
                .nouveauStatut(historique.getNouveauStatut())
                .dateChangement(historique.getDateChangement())
                .build();
    }
}
