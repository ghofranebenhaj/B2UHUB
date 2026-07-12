package com.b2uhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/api/auth/**",
                                "/h2-console/**",
                                "/ws/**",
                                "/api/health"
                        ).permitAll()
                        // Voir la liste des candidats d'une mission : réservé aux entreprises
                        .requestMatchers(HttpMethod.GET, "/api/candidatures/mission/**")
                        .hasRole("ENTREPRISE")
                        // Changer le statut d'une candidature (accepter/refuser) : entreprise
                        .requestMatchers(HttpMethod.PATCH, "/api/candidatures/*/statut")
                        .hasRole("ENTREPRISE")
                        // Postuler à une mission : réservé aux étudiants
                        .requestMatchers(HttpMethod.POST, "/api/candidatures")
                        .hasRole("ETUDIANT")
                        // Créer / modifier / supprimer une mission : entreprise
                        .requestMatchers(HttpMethod.POST, "/api/missions").hasRole("ENTREPRISE")
                        .requestMatchers(HttpMethod.PUT, "/api/missions/**").hasRole("ENTREPRISE")
                        .requestMatchers(HttpMethod.PATCH, "/api/missions/**").hasRole("ENTREPRISE")
                        .requestMatchers(HttpMethod.DELETE, "/api/missions/**").hasRole("ENTREPRISE")
                        // Le reste de l'API nécessite juste d'être authentifié
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
