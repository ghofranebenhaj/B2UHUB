package com.b2uhub.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AiMatchResponse {
    private Double similarity;
    private String methode;
    private List<String> competencesCommunes;
}
