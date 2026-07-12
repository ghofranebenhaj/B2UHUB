package com.b2uhub.config;

import com.b2uhub.model.Candidature;
import com.b2uhub.model.Entreprise;
import com.b2uhub.model.Etudiant;
import com.b2uhub.model.Mission;
import com.b2uhub.model.enums.CandidatureStatut;
import com.b2uhub.model.enums.MissionStatut;
import com.b2uhub.model.enums.RoleUtilisateur;
import com.b2uhub.repository.CandidatureRepository;
import com.b2uhub.repository.EntrepriseRepository;
import com.b2uhub.repository.EtudiantRepository;
import com.b2uhub.repository.MissionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
@Profile({"dev", "postgres"})
public class DataInitializer {

    // Mot de passe démo commun à tous les comptes seedés : "demo123"
    private static final String DEMO_PASSWORD_RAW = "demo123";

    @Bean
    CommandLineRunner seedData(
            EntrepriseRepository entrepriseRepository,
            EtudiantRepository etudiantRepository,
            MissionRepository missionRepository,
            CandidatureRepository candidatureRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (entrepriseRepository.count() == 0) {
                seedFullDemo(entrepriseRepository, etudiantRepository, missionRepository, candidatureRepository, passwordEncoder);
            } else if (candidatureRepository.count() == 0) {
                seedCandidaturesForDashboard(etudiantRepository, missionRepository, candidatureRepository);
            }
        };
    }

    private void seedFullDemo(
            EntrepriseRepository entrepriseRepository,
            EtudiantRepository etudiantRepository,
            MissionRepository missionRepository,
            CandidatureRepository candidatureRepository,
            PasswordEncoder passwordEncoder
    ) {
        Entreprise entreprise = new Entreprise();
        entreprise.setEmail("contact@techcorp.fr");
        entreprise.setMotDePasse(passwordEncoder.encode(DEMO_PASSWORD_RAW));
        entreprise.setNom("TechCorp");
        entreprise.setRole(RoleUtilisateur.ENTREPRISE);
        entreprise.setSecteur("IT");
        entreprise.setSiteWeb("https://techcorp.fr");
        entreprise = entrepriseRepository.save(entreprise);

        Etudiant alice = createEtudiant(
                "alice@univ.fr", "Alice Martin", "Informatique", 3,
                List.of("Java", "Angular", "PostgreSQL", "Docker"),
                1, 3, 72.0, 85, 15,
                "Développeuse full-stack — 3 projets web et mobile.",
                passwordEncoder
        );
        alice = etudiantRepository.save(alice);

        Etudiant bob = createEtudiant(
                "bob@univ.fr", "Bob Dupont", "Data Science", 2,
                List.of("Python", "Machine Learning", "FastAPI", "PostgreSQL"),
                2, 5, 88.0, 78, 20,
                "Data scientist — projets NLP et APIs Python.",
                passwordEncoder
        );
        bob = etudiantRepository.save(bob);

        Etudiant claire = createEtudiant(
                "claire@univ.fr", "Claire Leroy", "Génie logiciel", 4,
                List.of("Java", "Spring Boot", "React", "DevOps"),
                2, 4, 80.0, 90, 12,
                "Ingénieure logiciel — expérience microservices.",
                passwordEncoder
        );
        claire = etudiantRepository.save(claire);

        Mission m1 = saveMission(entreprise, missionRepository,
                "Application B2U-HUB",
                "Plateforme Angular + Spring Boot pour missions étudiantes.",
                MissionStatut.OUVERTE,
                List.of("Java", "Angular", "Spring Boot"), 12);

        Mission m2 = saveMission(entreprise, missionRepository,
                "Chatbot IA recrutement",
                "Assistant IA pour matching étudiants / entreprises.",
                MissionStatut.OUVERTE,
                List.of("Python", "FastAPI", "Machine Learning"), 8);

        Mission m3 = saveMission(entreprise, missionRepository,
                "Migration cloud DevOps",
                "Docker, CI/CD et monitoring Prometheus.",
                MissionStatut.EN_COURS,
                List.of("Docker", "Kubernetes", "DevOps"), 10);

        seedCandidatures(candidatureRepository, m1, m2, m3, alice, bob, claire);
    }

    private void seedCandidaturesForDashboard(
            EtudiantRepository etudiantRepository,
            MissionRepository missionRepository,
            CandidatureRepository candidatureRepository
    ) {
        List<Etudiant> etudiants = etudiantRepository.findAll();
        List<Mission> missions = missionRepository.findAll();
        if (etudiants.isEmpty() || missions.isEmpty()) return;

        Etudiant e1 = etudiants.get(0);
        Etudiant e2 = etudiants.size() > 1 ? etudiants.get(1) : e1;
        Mission m1 = missions.get(0);
        Mission m2 = missions.size() > 1 ? missions.get(1) : m1;
        Mission m3 = missions.size() > 2 ? missions.get(2) : m1;

        if (missions.size() >= 1 && missions.get(0).getStatut() != MissionStatut.EN_COURS) {
            missions.get(0).setStatut(MissionStatut.OUVERTE);
        }
        if (missions.size() >= 2) {
            missions.get(1).setStatut(MissionStatut.OUVERTE);
        }
        if (missions.size() >= 3) {
            missions.get(2).setStatut(MissionStatut.EN_COURS);
        }
        missionRepository.saveAll(missions);

        seedCandidatures(candidatureRepository, m1, m2, m3, e1, e2, e1);
    }

    private void seedCandidatures(
            CandidatureRepository candidatureRepository,
            Mission m1, Mission m2, Mission m3,
            Etudiant alice, Etudiant bob, Etudiant claire
    ) {
        saveCandidature(candidatureRepository, m1, alice, CandidatureStatut.ACCEPTEE, 84.5,
                "Compétences alignées Java/Angular — profil recommandé.");
        saveCandidature(candidatureRepository, m1, bob, CandidatureStatut.EN_ATTENTE, 62.0,
                "Profil data — matching partiel sur mission full-stack.");
        saveCandidature(candidatureRepository, m2, bob, CandidatureStatut.ACCEPTEE, 91.2,
                "Excellent match Python / ML / FastAPI.");
        saveCandidature(candidatureRepository, m2, claire, CandidatureStatut.EN_ATTENTE, 76.8,
                "Bon profil technique, en attente de validation.");
        saveCandidature(candidatureRepository, m3, alice, CandidatureStatut.EN_ATTENTE, 58.3,
                "Docker OK — expérience DevOps limitée.");
        saveCandidature(candidatureRepository, m3, claire, CandidatureStatut.ACCEPTEE, 88.0,
                "Profil DevOps / Spring — mission en cours.");
    }

    private Etudiant createEtudiant(
            String email, String nom, String filiere, int annee,
            List<String> competences, int exp, int projets, double perf,
            int soft, int dispo, String cv,
            PasswordEncoder passwordEncoder
    ) {
        Etudiant e = new Etudiant();
        e.setEmail(email);
        e.setMotDePasse(passwordEncoder.encode(DEMO_PASSWORD_RAW));
        e.setNom(nom);
        e.setRole(RoleUtilisateur.ETUDIANT);
        e.setFiliere(filiere);
        e.setAnneeEtude(annee);
        e.setCompetences(competences);
        e.setAnneesExperience(exp);
        e.setNombreProjetsRealises(projets);
        e.setPerformanceAnterieure(perf);
        e.setScoreSoftSkills(soft);
        e.setDisponibiliteHeuresSemaine(dispo);
        e.setCvTexte(cv);
        return e;
    }

    private Mission saveMission(
            Entreprise entreprise,
            MissionRepository repo,
            String titre, String desc, MissionStatut statut,
            List<String> competences, int semaines
    ) {
        Mission m = new Mission();
        m.setTitre(titre);
        m.setDescription(desc);
        m.setStatut(statut);
        m.setCompetencesRequises(competences);
        m.setDureeSemaines(semaines);
        m.setEntreprise(entreprise);
        return repo.save(m);
    }

    private void saveCandidature(
            CandidatureRepository repo,
            Mission mission, Etudiant etudiant,
            CandidatureStatut statut, double score, String explication
    ) {
        Candidature c = new Candidature();
        c.setMission(mission);
        c.setEtudiant(etudiant);
        c.setStatut(statut);
        c.setScoreIA(score);
        c.setExplicationScore(explication);
        repo.save(c);
    }
}
