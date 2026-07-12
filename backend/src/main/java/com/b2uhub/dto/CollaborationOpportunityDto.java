package com.b2uhub.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CollaborationOpportunityDto {
    private String type;
    private String titre;
    private List<String> acteurs;
    private String thematique;
    private Double scorePotentiel;
    private String description;
    private String actionSuggeree;
}
