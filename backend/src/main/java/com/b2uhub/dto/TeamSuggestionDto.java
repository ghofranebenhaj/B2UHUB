package com.b2uhub.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TeamSuggestionDto {
    private Long missionId;
    private List<String> membres;
    private Double couverture;
    private String explication;
}
