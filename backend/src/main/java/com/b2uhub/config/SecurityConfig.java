package com.b2uhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
@EnableMethodSecurity
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
                                "/api/health",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        // Lecture publique pour le mode démo frontend (sans JWT)
                        .requestMatchers(HttpMethod.GET,
                                "/api/analytics/**",
                                "/api/missions/**",
                                "/api/entreprises/**",
                                "/api/etudiants/**",
                                "/api/ai/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/candidatures/mission/**")
                        .hasAnyRole("ENTREPRISE", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/candidatures/*/statut")
                        .hasAnyRole("ENTREPRISE", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/candidatures")
                        .hasAnyRole("ETUDIANT", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/missions").hasAnyRole("ENTREPRISE", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/missions/**").hasAnyRole("ENTREPRISE", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/missions/**").hasAnyRole("ENTREPRISE", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/missions/**").hasAnyRole("ENTREPRISE", "ADMIN")
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
