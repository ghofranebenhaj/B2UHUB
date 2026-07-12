package com.b2uhub.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class EquipeResponse {
    private Long id;
    private String nom;
    private Long missionId;
    private String missionTitre;
    private Instant dateFormation;
    private Double couvertureCompetences;
    private String explication;
    private List<String> membresNoms;
}
