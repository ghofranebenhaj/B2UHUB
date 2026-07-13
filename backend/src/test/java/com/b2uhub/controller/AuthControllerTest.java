package com.b2uhub.controller;

import com.b2uhub.config.GlobalExceptionHandler;
import com.b2uhub.dto.AuthResponse;
import com.b2uhub.dto.LoginRequest;
import com.b2uhub.dto.RegisterRequest;
import com.b2uhub.model.enums.RoleUtilisateur;
import com.b2uhub.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void login_retourneToken() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .token("jwt-token")
                .userId(1L)
                .nom("Alice")
                .email("alice@test.fr")
                .role(RoleUtilisateur.ETUDIANT)
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@test.fr","motDePasse":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.role").value("ETUDIANT"));
    }

    @Test
    void register_retourne201() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .token("jwt-token")
                .userId(2L)
                .nom("TechCorp")
                .email("corp@test.fr")
                .role(RoleUtilisateur.ENTREPRISE)
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nom":"TechCorp",
                                  "email":"corp@test.fr",
                                  "motDePasse":"password123",
                                  "role":"ENTREPRISE",
                                  "secteur":"IT"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists());
    }
}
