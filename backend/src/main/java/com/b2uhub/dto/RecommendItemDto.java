package com.b2uhub.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecommendItemDto {
    private Long missionId;
    private String titre;
    private Double scoreMatching;
    private String raison;
}
