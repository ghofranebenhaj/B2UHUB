package com.b2uhub.controller;

import com.b2uhub.dto.EquipeResponse;
import com.b2uhub.service.EquipeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/equipes")
public class EquipeController {

    private final EquipeService equipeService;

    public EquipeController(EquipeService equipeService) {
        this.equipeService = equipeService;
    }

    @PostMapping("/mission/{missionId}")
    @ResponseStatus(HttpStatus.CREATED)
    public EquipeResponse formTeam(
            @PathVariable Long missionId,
            @RequestParam(defaultValue = "3") int taille
    ) {
        return equipeService.formTeam(missionId, taille);
    }

    @GetMapping("/mission/{missionId}")
    public EquipeResponse getByMission(@PathVariable Long missionId) {
        return equipeService.getByMission(missionId);
    }
}
