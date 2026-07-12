package com.b2uhub.dto;

import com.b2uhub.model.Mission;
import com.b2uhub.model.enums.MissionStatut;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class MissionResponse {

    private Long id;
    private String titre;
    private String description;
    private MissionStatut statut;
    private List<String> competencesRequises;
    private Integer dureeSemaines;
    private Instant datePublication;
    private Long entrepriseId;
    private String entrepriseNom;

    public static MissionResponse from(Mission mission) {
        return MissionResponse.builder()
                .id(mission.getId())
                .titre(mission.getTitre())
                .description(mission.getDescription())
                .statut(mission.getStatut())
                .competencesRequises(mission.getCompetencesRequises())
                .dureeSemaines(mission.getDureeSemaines())
                .datePublication(mission.getDatePublication())
                .entrepriseId(mission.getEntreprise().getId())
                .entrepriseNom(mission.getEntreprise().getNom())
                .build();
    }
}
