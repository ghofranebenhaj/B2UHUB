package com.b2uhub.dto;

import com.b2uhub.model.enums.MissionStatut;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MissionRequest {

    @NotBlank
    private String titre;

    @NotBlank
    private String description;

    private List<String> competencesRequises;

    private Integer dureeSemaines;

    private MissionStatut statut;

    @NotNull
    private Long entrepriseId;
}
