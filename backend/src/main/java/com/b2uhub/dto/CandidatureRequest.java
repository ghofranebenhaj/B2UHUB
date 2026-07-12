package com.b2uhub.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CandidatureRequest {

    @NotNull
    private Long missionId;

    @NotNull
    private Long etudiantId;
}
