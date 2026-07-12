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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test d'intégration bout-en-bout : inscription, login JWT, candidature,
 * scoring IA (fallback local car ai-service n'est pas démarré en test),
 * consultation des candidatures par l'entreprise, et acceptation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CandidatureIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void flowCompletCandidatureEtAcceptation() throws Exception {
        // 1. Inscription + login d'une entreprise
        String entrepriseRegisterBody = """
                {
                  "nom": "TechCorp Test",
                  "email": "entreprise@test.fr",
                  "motDePasse": "password123",
                  "role": "ENTREPRISE",
                  "secteur": "IT"
                }
                """;
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entrepriseRegisterBody))
                .andExpect(status().isCreated());

        String entrepriseLoginBody = """
                {"email": "entreprise@test.fr", "motDePasse": "password123"}
                """;
        MvcResult entrepriseLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entrepriseLoginBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode entrepriseAuth = objectMapper.readTree(entrepriseLoginResult.getResponse().getContentAsString());
        String entrepriseToken = entrepriseAuth.get("token").asText();
        long entrepriseId = entrepriseAuth.get("userId").asLong();

        // 2. Inscription + login d'un étudiant
        String etudiantRegisterBody = """
                {
                  "nom": "Etudiant Test",
                  "email": "etudiant@test.fr",
                  "motDePasse": "password123",
                  "role": "ETUDIANT",
                  "filiere": "Informatique"
                }
                """;
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(etudiantRegisterBody))
                .andExpect(status().isCreated());

        String etudiantLoginBody = """
                {"email": "etudiant@test.fr", "motDePasse": "password123"}
                """;
        MvcResult etudiantLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(etudiantLoginBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode etudiantAuth = objectMapper.readTree(etudiantLoginResult.getResponse().getContentAsString());
        String etudiantToken = etudiantAuth.get("token").asText();
        long etudiantId = etudiantAuth.get("userId").asLong();

        // 3. L'entreprise crée une mission
        String missionBody = """
                {
                  "titre": "Mission Intégration",
                  "description": "Test end-to-end",
                  "competencesRequises": ["Java", "Spring"],
                  "dureeSemaines": 8,
                  "entrepriseId": %d
                }
                """.formatted(entrepriseId);

        MvcResult missionResult = mockMvc.perform(post("/api/missions")
                        .header("Authorization", "Bearer " + entrepriseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(missionBody))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode missionJson = objectMapper.readTree(missionResult.getResponse().getContentAsString());
        long missionId = missionJson.get("id").asLong();

        // 4. L'étudiant postule -> vérifie un score IA renvoyé (fallback local, ai-service non démarré)
        String candidatureBody = """
                {"missionId": %d, "etudiantId": %d}
                """.formatted(missionId, etudiantId);

        MvcResult candidatureResult = mockMvc.perform(post("/api/candidatures")
                        .header("Authorization", "Bearer " + etudiantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(candidatureBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scoreIA").exists())
                .andReturn();

        JsonNode candidatureJson = objectMapper.readTree(candidatureResult.getResponse().getContentAsString());
        long candidatureId = candidatureJson.get("id").asLong();

        // 5. Un étudiant ne peut PAS voir la liste des candidats (réservé à l'entreprise) -> 403
        mockMvc.perform(get("/api/candidatures/mission/" + missionId)
                        .header("Authorization", "Bearer " + etudiantToken))
                .andExpect(status().isForbidden());

        // 6. L'entreprise propriétaire voit bien la candidature
        mockMvc.perform(get("/api/candidatures/mission/" + missionId)
                        .header("Authorization", "Bearer " + entrepriseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(candidatureId));

        // 7. L'entreprise accepte la candidature
        mockMvc.perform(patch("/api/candidatures/" + candidatureId + "/statut")
                        .header("Authorization", "Bearer " + entrepriseToken)
                        .param("statut", "ACCEPTEE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ACCEPTEE"));

        // 8. L'historique de la candidature contient bien le changement de statut
        MvcResult historiqueResult = mockMvc.perform(get("/api/candidatures/" + candidatureId + "/historique")
                        .header("Authorization", "Bearer " + entrepriseToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode historique = objectMapper.readTree(historiqueResult.getResponse().getContentAsString());
        assertThat(historique.isArray()).isTrue();
        assertThat(historique.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void postulerSansToken_doitEchouer() throws Exception {
        String candidatureBody = """
                {"missionId": 1, "etudiantId": 1}
                """;
        mockMvc.perform(post("/api/candidatures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(candidatureBody))
                .andExpect(status().isForbidden());
    }
}
