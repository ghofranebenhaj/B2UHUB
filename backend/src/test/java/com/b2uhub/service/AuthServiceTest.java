package com.b2uhub.service;

import com.b2uhub.config.JwtService;
import com.b2uhub.dto.AuthResponse;
import com.b2uhub.dto.LoginRequest;
import com.b2uhub.dto.RegisterRequest;
import com.b2uhub.model.Entreprise;
import com.b2uhub.model.Etudiant;
import com.b2uhub.model.Utilisateur;
import com.b2uhub.model.enums.RoleUtilisateur;
import com.b2uhub.repository.EntrepriseRepository;
import com.b2uhub.repository.EtudiantRepository;
import com.b2uhub.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private EtudiantRepository etudiantRepository;
    @Mock
    private EntrepriseRepository entrepriseRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        lenient().when(jwtService.generateToken(any())).thenReturn("fake-jwt-token");
    }

    @Test
    void register_emailDejaUtilise_doitEchouer() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existe@test.fr");
        request.setRole(RoleUtilisateur.ETUDIANT);

        when(utilisateurRepository.existsByEmail("existe@test.fr")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("existe déjà");

        verify(etudiantRepository, never()).save(any());
    }

    @Test
    void register_roleAdmin_doitEchouer() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("admin@test.fr");
        request.setRole(RoleUtilisateur.ADMIN);

        when(utilisateurRepository.existsByEmail("admin@test.fr")).thenReturn(false);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ADMIN");
    }

    @Test
    void register_etudiant_succes() {
        RegisterRequest request = new RegisterRequest();
        request.setNom("Alice");
        request.setEmail("alice@test.fr");
        request.setMotDePasse("password123");
        request.setRole(RoleUtilisateur.ETUDIANT);
        request.setFiliere("Informatique");
        request.setAnneeEtude(3);

        when(utilisateurRepository.existsByEmail("alice@test.fr")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(etudiantRepository.save(any(Etudiant.class))).thenAnswer(inv -> {
            Etudiant e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        AuthResponse response = authService.register(request);

        assertThat(response.getToken()).isEqualTo("fake-jwt-token");
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getRole()).isEqualTo(RoleUtilisateur.ETUDIANT);
        verify(entrepriseRepository, never()).save(any());
    }

    @Test
    void register_entreprise_succes() {
        RegisterRequest request = new RegisterRequest();
        request.setNom("TechCorp");
        request.setEmail("contact@techcorp.fr");
        request.setMotDePasse("password123");
        request.setRole(RoleUtilisateur.ENTREPRISE);
        request.setSecteur("IT");

        when(utilisateurRepository.existsByEmail("contact@techcorp.fr")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(entrepriseRepository.save(any(Entreprise.class))).thenAnswer(inv -> {
            Entreprise e = inv.getArgument(0);
            e.setId(2L);
            return e;
        });

        AuthResponse response = authService.register(request);

        assertThat(response.getRole()).isEqualTo(RoleUtilisateur.ENTREPRISE);
        verify(etudiantRepository, never()).save(any());
    }

    @Test
    void login_emailInconnu_doitEchouer() {
        LoginRequest request = new LoginRequest();
        request.setEmail("inconnu@test.fr");
        request.setMotDePasse("password123");

        when(utilisateurRepository.findByEmail("inconnu@test.fr")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_motDePasseIncorrect_doitEchouer() {
        Etudiant etudiant = new Etudiant();
        etudiant.setEmail("alice@test.fr");
        etudiant.setMotDePasse("hashed-password");

        LoginRequest request = new LoginRequest();
        request.setEmail("alice@test.fr");
        request.setMotDePasse("mauvais-mot-de-passe");

        when(utilisateurRepository.findByEmail("alice@test.fr")).thenReturn(Optional.of(etudiant));
        when(passwordEncoder.matches("mauvais-mot-de-passe", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_succes_retourneUnTokenEtLesInfosUtilisateur() {
        Etudiant etudiant = new Etudiant();
        etudiant.setId(1L);
        etudiant.setNom("Alice");
        etudiant.setEmail("alice@test.fr");
        etudiant.setMotDePasse("hashed-password");
        etudiant.setRole(RoleUtilisateur.ETUDIANT);

        LoginRequest request = new LoginRequest();
        request.setEmail("alice@test.fr");
        request.setMotDePasse("password123");

        when(utilisateurRepository.findByEmail("alice@test.fr")).thenReturn(Optional.of((Utilisateur) etudiant));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("fake-jwt-token");
        assertThat(response.getEmail()).isEqualTo("alice@test.fr");
        assertThat(response.getRole()).isEqualTo(RoleUtilisateur.ETUDIANT);
    }
}
