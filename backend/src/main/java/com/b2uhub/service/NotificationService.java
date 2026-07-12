package com.b2uhub.service;

import com.b2uhub.model.Notification;
import com.b2uhub.model.Utilisateur;
import com.b2uhub.repository.NotificationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class NotificationService {

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

    private void pushWebSocket(Long userId, String titre, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("titre", titre);
        payload.put("message", message);
        payload.put("utilisateurId", userId);
        try {
            messagingTemplate.convertAndSend("/topic/notifications/" + userId, objectMapper.writeValueAsString(payload));
            messagingTemplate.convertAndSend("/topic/notifications", objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException ignored) {
        }
    }
}
