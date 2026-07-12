package com.b2uhub.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnalyticsSummaryResponse {

    private long missionsOuvertes;
    private long missionsEnCours;
    private long candidaturesEnAttente;
    private long candidaturesAcceptees;
    private double scoreMoyen;
}
