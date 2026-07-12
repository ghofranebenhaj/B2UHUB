package com.b2uhub.dto;

import com.b2uhub.model.Entreprise;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EntrepriseResponse {

    private Long id;
    private String nom;
    private String email;
    private String secteur;

    public static EntrepriseResponse from(Entreprise entreprise) {
        return EntrepriseResponse.builder()
                .id(entreprise.getId())
                .nom(entreprise.getNom())
                .email(entreprise.getEmail())
                .secteur(entreprise.getSecteur())
                .build();
    }
}
