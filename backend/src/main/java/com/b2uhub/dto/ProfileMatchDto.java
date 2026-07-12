package com.b2uhub.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProfileMatchDto {
    private Long missionId;
    private String missionTitre;
    private String entreprise;
    private Double scoreMatching;
    private List<String> competencesAlignees;
    private String recommandation;
    private String justification;
}
