package com.b2uhub.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Client OpenFeign vers le microservice ai-service (FastAPI).
 * Remplace les appels RestClient précédemment faits à la main dans AiServiceClient.
 * L'URL de base est résolue depuis la propriété b2u.ai-service.base-url
 * via la configuration FeignAiServiceConfig.
 */
@FeignClient(name = "ai-service", url = "${b2u.ai-service.base-url}")
public interface AiServiceFeignClient {

    @GetMapping("/api/health")
    Map<String, Object> health();

    @PostMapping("/api/v1/score")
    Map<String, Object> score(@RequestBody Map<String, Object> body);

    @PostMapping("/api/v1/match")
    Map<String, Object> match(@RequestBody Map<String, Object> body);

    @PostMapping("/api/v1/recommend")
    Map<String, Object> recommend(@RequestBody Map<String, Object> body);

    @PostMapping("/api/v1/team")
    Map<String, Object> team(@RequestBody Map<String, Object> body);

    @PostMapping("/api/v1/cv/analyze")
    Map<String, Object> analyzeCv(@RequestBody Map<String, Object> body);

    @PostMapping("/api/v1/agents/smart-matching")
    Map<String, Object> smartMatching(@RequestBody Map<String, Object> body);

    @PostMapping("/api/v1/agents/collaboration")
    Map<String, Object> collaboration(@RequestBody Map<String, Object> body);
}
