package com.b2uhub.support;

import com.b2uhub.model.enums.RoleUtilisateur;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

public final class SecurityTestUtils {

    private SecurityTestUtils() {
    }

    public static void authenticateAs(Long userId, RoleUtilisateur role) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    public static void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }
}
