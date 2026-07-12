package com.b2uhub.controller;

import com.b2uhub.dto.CandidatureHistoriqueResponse;
import com.b2uhub.dto.CandidatureRequest;
import com.b2uhub.dto.CandidatureResponse;
import com.b2uhub.model.enums.CandidatureStatut;
import com.b2uhub.service.CandidatureService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/candidatures")
public class CandidatureController {

    private final CandidatureService candidatureService;

    public CandidatureController(CandidatureService candidatureService) {
        this.candidatureService = candidatureService;
    }

    @GetMapping("/mission/{missionId}")
    public List<CandidatureResponse> byMission(@PathVariable Long missionId) {
        return candidatureService.findByMission(missionId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CandidatureResponse postuler(@Valid @RequestBody CandidatureRequest request) {
        return candidatureService.postuler(request);
    }

    @PatchMapping("/{id}/statut")
    public CandidatureResponse updateStatut(@PathVariable Long id, @RequestParam CandidatureStatut statut) {
        return candidatureService.updateStatut(id, statut);
    }

    @GetMapping("/{id}/historique")
    public List<CandidatureHistoriqueResponse> historique(@PathVariable Long id) {
        return candidatureService.getHistorique(id);
    }
}
