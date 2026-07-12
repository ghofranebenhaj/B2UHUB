package com.b2uhub.dto;

import com.b2uhub.model.enums.RoleUtilisateur;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private String token;
    private Long userId;
    private String nom;
    private String email;
    private RoleUtilisateur role;
}
