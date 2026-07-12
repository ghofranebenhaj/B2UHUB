package com.b2uhub.config;

import com.b2uhub.model.Entreprise;
import com.b2uhub.model.enums.RoleUtilisateur;
import com.b2uhub.repository.EntrepriseRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EntrepriseEnsureConfig {

    @Bean
    CommandLineRunner ensureDefaultEntreprise(EntrepriseRepository entrepriseRepository) {
        return args -> {
            if (entrepriseRepository.count() > 0) {
                return;
            }
            Entreprise e = new Entreprise();
            e.setEmail("contact@techcorp.fr");
            e.setMotDePasse("demo");
            e.setNom("TechCorp");
            e.setRole(RoleUtilisateur.ENTREPRISE);
            e.setSecteur("IT");
            e.setSiteWeb("https://techcorp.fr");
            entrepriseRepository.save(e);
        };
    }
}
