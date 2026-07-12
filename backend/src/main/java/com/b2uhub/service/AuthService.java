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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final EtudiantRepository etudiantRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UtilisateurRepository utilisateurRepository,
            EtudiantRepository etudiantRepository,
            EntrepriseRepository entrepriseRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.utilisateurRepository = utilisateurRepository;
        this.etudiantRepository = etudiantRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (utilisateurRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Un compte existe déjà avec cet email");
        }
        if (request.getRole() == RoleUtilisateur.ADMIN) {
            throw new BadRequestException("Inscription ADMIN non autorisée via cet endpoint");
        }

        Utilisateur utilisateur;

        if (request.getRole() == RoleUtilisateur.ETUDIANT) {
            Etudiant etudiant = new Etudiant();
            etudiant.setFiliere(request.getFiliere());
            etudiant.setAnneeEtude(request.getAnneeEtude());
            utilisateur = etudiant;
        } else {
            Entreprise entreprise = new Entreprise();
            entreprise.setSecteur(request.getSecteur());
            entreprise.setSiteWeb(request.getSiteWeb());
            utilisateur = entreprise;
        }

        utilisateur.setNom(request.getNom());
        utilisateur.setEmail(request.getEmail());
        utilisateur.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
        utilisateur.setRole(request.getRole());

        Utilisateur saved = request.getRole() == RoleUtilisateur.ETUDIANT
                ? etudiantRepository.save((Etudiant) utilisateur)
                : entrepriseRepository.save((Entreprise) utilisateur);

        return buildAuthResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Email ou mot de passe incorrect"));

        if (!passwordEncoder.matches(request.getMotDePasse(), utilisateur.getMotDePasse())) {
            throw new UnauthorizedException("Email ou mot de passe incorrect");
        }

        return buildAuthResponse(utilisateur);
    }

    private AuthResponse buildAuthResponse(Utilisateur utilisateur) {
        return AuthResponse.builder()
                .token(jwtService.generateToken(utilisateur))
                .userId(utilisateur.getId())
                .nom(utilisateur.getNom())
                .email(utilisateur.getEmail())
                .role(utilisateur.getRole())
                .build();
    }
}
