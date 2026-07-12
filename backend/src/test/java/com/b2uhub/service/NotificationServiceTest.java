package com.b2uhub.service;

import com.b2uhub.model.Etudiant;
import com.b2uhub.model.Notification;
import com.b2uhub.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private NotificationService notificationService;

    private Etudiant etudiant;

    @BeforeEach
    void setUp() {
        // ObjectMapper réel : pas besoin de mocker la sérialisation JSON du payload WebSocket
        notificationService = new NotificationService(notificationRepository, messagingTemplate, new ObjectMapper());

        etudiant = new Etudiant();
        etudiant.setId(100L);
        etudiant.setNom("Alice");
    }

    @Test
    void notify_sauvegardeEnBaseEtPousseSurLesDeuxTopicsWebSocket() {
        notificationService.notify(etudiant, "Titre", "Message de test");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getTitre()).isEqualTo("Titre");
        assertThat(saved.getMessage()).isEqualTo("Message de test");
        assertThat(saved.getUtilisateur()).isEqualTo(etudiant);

        verify(messagingTemplate).convertAndSend(eq("/topic/notifications/100"), anyString());
        verify(messagingTemplate).convertAndSend(eq("/topic/notifications"), anyString());
    }

    @Test
    void notify_erreurWebSocket_neBloquePasLaSauvegarde() {
        doThrow(new RuntimeException("connexion perdue"))
                .when(messagingTemplate).convertAndSend(anyString(), anyString());

        // ne doit pas lever d'exception malgré l'échec du push WebSocket
        notificationService.notify(etudiant, "Titre", "Message");

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void findByUser_delegueAuRepositoryTrieParDateDesc() {
        Notification n1 = new Notification();
        n1.setTitre("Notif 1");
        when(notificationRepository.findByUtilisateurIdOrderByDateEnvoiDesc(100L))
                .thenReturn(List.of(n1));

        List<Notification> result = notificationService.findByUser(100L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitre()).isEqualTo("Notif 1");
    }
}
