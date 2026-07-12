package com.b2uhub.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test d'intégration : inscription, connexion, rejet des identifiants
 * invalides et protection des endpoints par JWT (Spring Security).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void inscriptionPuisConnexion_succes() throws Exception {
        String registerBody = """
                {
                  "nom": "Bob",
                  "email": "bob.auth@test.fr",
                  "motDePasse": "password123",
                  "role": "ETUDIANT",
                  "filiere": "Informatique"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("ETUDIANT"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "bob.auth@test.fr", "motDePasse": "password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void inscriptionAvecEmailDejaUtilise_doitEchouer() throws Exception {
        String registerBody = """
                {
                  "nom": "Charlie",
                  "email": "charlie.auth@test.fr",
                  "motDePasse": "password123",
                  "role": "ETUDIANT"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isConflict());
    }

    @Test
    void connexionAvecMauvaisMotDePasse_doitEchouer() throws Exception {
        String registerBody = """
                {
                  "nom": "Dora",
                  "email": "dora.auth@test.fr",
                  "motDePasse": "password123",
                  "role": "ENTREPRISE",
                  "secteur": "Finance"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "dora.auth@test.fr", "motDePasse": "mauvais-mdp"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void inscriptionEnTantQuAdmin_doitEtreRefusee() throws Exception {
        String registerBody = """
                {
                  "nom": "Admin",
                  "email": "admin.auth@test.fr",
                  "motDePasse": "password123",
                  "role": "ADMIN"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void endpointProtege_sansToken_doitRenvoyer403() throws Exception {
        // toute route /api/** non explicitement listée en "permitAll" exige un token
        mockMvc.perform(get("/api/missions"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/missions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titre": "x", "description": "x", "entrepriseId": 1}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void endpointAuth_estPublicMemeSansToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "inexistant@test.fr", "motDePasse": "x"}
                                """))
                .andExpect(status().isUnauthorized()); // 401, pas 403 : la route est bien atteinte
    }
}
