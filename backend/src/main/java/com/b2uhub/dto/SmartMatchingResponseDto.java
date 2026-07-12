package com.b2uhub.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SmartMatchingResponseDto {
    private String agent;
    private String message;
    private String methode;
    private List<ProfileMatchDto> profilsRecommandes;
    private TeamSuggestionDto suggestionEquipe;
}
