package com.b2uhub.service;

import com.b2uhub.model.Notification;
import com.b2uhub.model.Utilisateur;
import com.b2uhub.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public NotificationService(
            NotificationRepository notificationRepository,
            SimpMessagingTemplate messagingTemplate,
            ObjectMapper objectMapper
    ) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    public void notify(Utilisateur utilisateur, String titre, String message) {
        Notification n = new Notification();
        n.setUtilisateur(utilisateur);
        n.setTitre(titre);
        n.setMessage(message);
        notificationRepository.save(n);
        pushWebSocket(utilisateur.getId(), titre, message);
    }

    public List<Notification> findByUser(Long userId) {
        return notificationRepository.findByUtilisateurIdOrderByDateEnvoiDesc(userId);
    }

    public Notification markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification introuvable: " + notificationId));
        if (!notification.getUtilisateur().getId().equals(userId)) {
            throw new ForbiddenException("Cette notification ne vous appartient pas");
        }
        notification.setLue(true);
        log.debug("Notification id={} marquée comme lue pour utilisateur id={}", notificationId, userId);
        return notification;
    }

    private void pushWebSocket(Long userId, String titre, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("titre", titre);
        payload.put("message", message);
        payload.put("utilisateurId", userId);
        try {
            messagingTemplate.convertAndSend("/topic/notifications/" + userId, objectMapper.writeValueAsString(payload));
            messagingTemplate.convertAndSend("/topic/notifications", objectMapper.writeValueAsString(payload));
        } catch (Exception ex) {
            log.warn("Échec envoi WebSocket pour utilisateur id={}: {}", userId, ex.getMessage());
        }
    }
}
