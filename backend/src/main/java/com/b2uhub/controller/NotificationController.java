package com.b2uhub.controller;

import com.b2uhub.dto.NotificationResponse;
import com.b2uhub.security.SecurityUtils;
import com.b2uhub.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Consultation et gestion des notifications utilisateur")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Lister les notifications de l'utilisateur connecté")
    public List<NotificationResponse> listMine() {
        Long userId = SecurityUtils.getCurrentUserId();
        return notificationService.findByUser(userId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @PatchMapping("/{id}/lue")
    @Operation(summary = "Marquer une notification comme lue")
    public NotificationResponse markAsRead(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return NotificationResponse.from(notificationService.markAsRead(id, userId));
    }
}
