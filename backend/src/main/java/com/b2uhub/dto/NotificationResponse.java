package com.b2uhub.dto;

import com.b2uhub.model.Notification;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class NotificationResponse {

    private final Long id;
    private final String titre;
    private final String message;
    private final boolean lue;
    private final Instant dateEnvoi;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .titre(notification.getTitre())
                .message(notification.getMessage())
                .lue(notification.isLue())
                .dateEnvoi(notification.getDateEnvoi())
                .build();
    }
}
