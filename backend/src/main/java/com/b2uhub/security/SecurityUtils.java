package com.b2uhub.security;

import com.b2uhub.model.Mission;
import com.b2uhub.model.enums.RoleUtilisateur;
import com.b2uhub.service.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new ForbiddenException("Utilisateur non authentifié");
        }
        if (auth.getPrincipal() instanceof Long userId) {
            return userId;
        }
        throw new ForbiddenException("Session invalide");
    }

    public static boolean hasRole(RoleUtilisateur role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        String authority = "ROLE_" + role.name();
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }

    public static boolean isAdmin() {
        return hasRole(RoleUtilisateur.ADMIN);
    }

    public static void requireEntrepriseOwnerOfMission(Mission mission) {
        if (isAdmin()) {
            return;
        }
        if (!hasRole(RoleUtilisateur.ENTREPRISE)) {
            throw new ForbiddenException("Accès réservé à l'entreprise propriétaire de la mission");
        }
        Long currentUserId = getCurrentUserId();
        if (mission.getEntreprise() == null || !mission.getEntreprise().getId().equals(currentUserId)) {
            throw new ForbiddenException("Vous n'êtes pas propriétaire de cette mission");
        }
    }

    public static void requireEtudiantSelf(Long etudiantId) {
        if (isAdmin()) {
            return;
        }
        if (!hasRole(RoleUtilisateur.ETUDIANT)) {
            throw new ForbiddenException("Accès réservé aux étudiants");
        }
        if (!getCurrentUserId().equals(etudiantId)) {
            throw new ForbiddenException("Vous ne pouvez agir que pour votre propre compte");
        }
    }

    public static void requireEntrepriseSelf(Long entrepriseId) {
        if (isAdmin()) {
            return;
        }
        if (!hasRole(RoleUtilisateur.ENTREPRISE)) {
            throw new ForbiddenException("Accès réservé aux entreprises");
        }
        if (!getCurrentUserId().equals(entrepriseId)) {
            throw new ForbiddenException("Vous ne pouvez agir que pour votre propre entreprise");
        }
    }
}
