package com.b2uhub.controller;

import com.b2uhub.dto.*;
import com.b2uhub.service.AiHubService;
import com.b2uhub.service.AiServiceClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiHubService aiHubService;

    public AiController(AiHubService aiHubService) {
        this.aiHubService = aiHubService;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return aiHubService.status();
    }

    @GetMapping("/recommend/{etudiantId}")
    public List<RecommendItemDto> recommend(
            @PathVariable Long etudiantId,
            @RequestParam(defaultValue = "5") int topK
    ) {
        return aiHubService.recommendForEtudiant(etudiantId, topK);
    }

    @GetMapping("/match")
    public AiMatchResponse match(
            @RequestParam Long etudiantId,
            @RequestParam Long missionId
    ) {
        return aiHubService.match(etudiantId, missionId);
    }

    @GetMapping("/cv/{etudiantId}")
    public AiServiceClient.CvAnalysisResult analyzeCv(@PathVariable Long etudiantId) {
        return aiHubService.analyzeCv(etudiantId);
    }

    @GetMapping("/agents/smart-matching/{etudiantId}")
    public SmartMatchingResponseDto smartMatching(
            @PathVariable Long etudiantId,
            @RequestParam(required = false) Long missionId
    ) {
        return aiHubService.runSmartMatchingAgent(etudiantId, missionId);
    }

    @GetMapping("/agents/collaboration")
    public CollaborationResponseDto collaboration() {
        return aiHubService.runCollaborationAgent();
    }
}
