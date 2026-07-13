package com.b2uhub.dto;

import com.b2uhub.model.enums.RoleUtilisateur;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    private String nom;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String motDePasse;

    @NotNull
    private RoleUtilisateur role;

    // Champs optionnels spécifiques Étudiant
    private String filiere;
    private Integer anneeEtude;

    // Champs optionnels spécifiques Entreprise
    private String secteur;
    private String siteWeb;
}
