package com.b2uhub.controller;

import com.b2uhub.model.Etudiant;
import com.b2uhub.repository.EtudiantRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/etudiants")
public class EtudiantController {

    private final EtudiantRepository etudiantRepository;

    public EtudiantController(EtudiantRepository etudiantRepository) {
        this.etudiantRepository = etudiantRepository;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return etudiantRepository.findAll().stream()
                .map(e -> Map.<String, Object>of(
                        "id", e.getId(),
                        "nom", e.getNom(),
                        "email", e.getEmail(),
                        "filiere", e.getFiliere() != null ? e.getFiliere() : "",
                        "competences", e.getCompetences()
                ))
                .toList();
    }
}
