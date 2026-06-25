package ar.edu.unnoba.pdyc2026.notification.dto;

import ar.edu.unnoba.pdyc2026.notification.model.Notification;
import java.time.LocalDateTime;

public class NotificationDTO {
    private Long id;
    private String message;
    private Long eventId;
    private boolean read;
    private LocalDateTime createdAt;

    public static NotificationDTO fromEntity(Notification n) {
        NotificationDTO dto = new NotificationDTO();
        dto.id = n.getId();
        dto.message = n.getMessage();
        dto.eventId = n.getEventId();
        dto.read = n.isRead();
        dto.createdAt = n.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public String getMessage() { return message; }
    public Long getEventId() { return eventId; }
    public boolean isRead() { return read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
