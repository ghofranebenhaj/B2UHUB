package com.b2uhub.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test d'intégration : création/consultation/clôture d'une mission,
 * en vérifiant les restrictions de rôles (ENTREPRISE vs ETUDIANT)
 * appliquées par Spring Security.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MissionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndLogin(String email, String role, String extraFields) throws Exception {
        String registerBody = """
                {
                  "nom": "User %s",
                  "email": "%s",
                  "motDePasse": "password123",
                  "role": "%s"
                  %s
                }
                """.formatted(email, email, role, extraFields);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = """
                {"email": "%s", "motDePasse": "password123"}
                """.formatted(email);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    @Test
    void etudiantNePeutPasCreerDeMission() throws Exception {
        String etudiantToken = registerAndLogin(
                "etudiant.mission@test.fr", "ETUDIANT", ", \"filiere\": \"Info\"");

        String missionBody = """
                {"titre": "Mission interdite", "description": "test", "entrepriseId": 1}
                """;

        mockMvc.perform(post("/api/missions")
                        .header("Authorization", "Bearer " + etudiantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(missionBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void entrepriseCreeUneMission_visibleDansLaListePublique() throws Exception {
        String entrepriseToken = registerAndLogin(
                "entreprise.mission@test.fr", "ENTREPRISE", ", \"secteur\": \"IT\"");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "entreprise.mission@test.fr", "motDePasse": "password123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        long entrepriseId = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("userId").asLong();

        String missionBody = """
                {
                  "titre": "Développeur Angular",
                  "description": "Mission front-end",
                  "competencesRequises": ["Angular", "TypeScript"],
                  "dureeSemaines": 10,
                  "entrepriseId": %d
                }
                """.formatted(entrepriseId);

        MvcResult createResult = mockMvc.perform(post("/api/missions")
                        .header("Authorization", "Bearer " + entrepriseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(missionBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("OUVERTE"))
                .andReturn();

        long missionId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();

        // consultation : nécessite un utilisateur authentifié (règle "/api/**" -> authenticated())
        mockMvc.perform(get("/api/missions/" + missionId)
                        .header("Authorization", "Bearer " + entrepriseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titre").value("Développeur Angular"));

        mockMvc.perform(get("/api/missions")
                        .header("Authorization", "Bearer " + entrepriseToken)
                        .param("competence", "Angular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + missionId + ")]").exists());

        // sans token du tout : rejeté
        mockMvc.perform(get("/api/missions/" + missionId))
                .andExpect(status().isForbidden());
    }

    @Test
    void cloturerUneMissionSansCandidatureAcceptee_doitEchouer() throws Exception {
        String entrepriseToken = registerAndLogin(
                "entreprise.cloture@test.fr", "ENTREPRISE", ", \"secteur\": \"IT\"");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "entreprise.cloture@test.fr", "motDePasse": "password123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        long entrepriseId = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("userId").asLong();

        String missionBody = """
                {"titre": "Mission sans candidat", "description": "test", "entrepriseId": %d}
                """.formatted(entrepriseId);

        MvcResult createResult = mockMvc.perform(post("/api/missions")
                        .header("Authorization", "Bearer " + entrepriseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(missionBody))
                .andExpect(status().isCreated())
                .andReturn();
        long missionId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(patch("/api/missions/" + missionId + "/statut")
                        .header("Authorization", "Bearer " + entrepriseToken)
                        .param("statut", "CLOTUREE"))
                .andExpect(status().isBadRequest());
    }
}
