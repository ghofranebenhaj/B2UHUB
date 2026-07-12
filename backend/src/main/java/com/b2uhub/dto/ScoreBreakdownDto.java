package com.b2uhub.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScoreBreakdownDto {
    private String critere;
    private Double note;
    private Double poids;
    private Double contribution;
    private String detail;
}
