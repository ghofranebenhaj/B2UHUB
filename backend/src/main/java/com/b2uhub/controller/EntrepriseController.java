package com.b2uhub.controller;

import com.b2uhub.dto.EntrepriseResponse;
import com.b2uhub.repository.EntrepriseRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/entreprises")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class EntrepriseController {

    private final EntrepriseRepository entrepriseRepository;

    public EntrepriseController(EntrepriseRepository entrepriseRepository) {
        this.entrepriseRepository = entrepriseRepository;
    }

    @GetMapping
    public List<EntrepriseResponse> list() {
        return entrepriseRepository.findAll().stream()
                .map(EntrepriseResponse::from)
                .toList();
    }
}
