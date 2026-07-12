package com.b2uhub.repository;

import com.b2uhub.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUtilisateurIdOrderByDateEnvoiDesc(Long utilisateurId);
}
