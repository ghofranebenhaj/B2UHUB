package com.b2uhub.controller;

import com.b2uhub.dto.CountResponse;
import com.b2uhub.dto.EntrepriseResponse;
import com.b2uhub.dto.MissionRequest;
import com.b2uhub.dto.MissionResponse;
import com.b2uhub.model.enums.MissionStatut;
import com.b2uhub.repository.EntrepriseRepository;
import com.b2uhub.service.CandidatureService;
import com.b2uhub.service.MissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/missions")
public class MissionController {

    private final MissionService missionService;
    private final EntrepriseRepository entrepriseRepository;
    private final CandidatureService candidatureService;

    public MissionController(
            MissionService missionService,
            EntrepriseRepository entrepriseRepository,
            CandidatureService candidatureService
    ) {
        this.missionService = missionService;
        this.entrepriseRepository = entrepriseRepository;
        this.candidatureService = candidatureService;
    }

    /** Liste entreprises (alias si /api/entreprises bloqué) */
    @GetMapping("/entreprises")
    public List<EntrepriseResponse> listEntreprises() {
        return entrepriseRepository.findAll().stream()
                .map(EntrepriseResponse::from)
                .toList();
    }

    @GetMapping
    public List<MissionResponse> list(
            @RequestParam(required = false) MissionStatut statut,
            @RequestParam(required = false) String competence,
            @RequestParam(required = false) String titre
    ) {
        return missionService.findAll(statut, competence, titre);
    }

    @GetMapping("/{id}/candidatures/count")
    public CountResponse countCandidatures(@PathVariable Long id) {
        return new CountResponse(candidatureService.countByMission(id));
    }

    @GetMapping("/{id}")
    public MissionResponse get(@PathVariable Long id) {
        return missionService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MissionResponse create(@Valid @RequestBody MissionRequest request) {
        return missionService.create(request);
    }

    @PutMapping("/{id}")
    public MissionResponse update(@PathVariable Long id, @Valid @RequestBody MissionRequest request) {
        return missionService.update(id, request);
    }

    @PatchMapping("/{id}/statut")
    public MissionResponse updateStatut(@PathVariable Long id, @RequestParam MissionStatut statut) {
        return missionService.updateStatut(id, statut);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        missionService.delete(id);
    }
}
