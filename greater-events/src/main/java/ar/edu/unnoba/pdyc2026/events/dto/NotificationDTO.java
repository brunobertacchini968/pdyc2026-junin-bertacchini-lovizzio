package ar.edu.unnoba.pdyc2026.events.dto;

import ar.edu.unnoba.pdyc2026.events.model.Notification;
import java.time.LocalDateTime;

public class NotificationDTO {

    private Long id;
    private Long eventId;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;

    public static NotificationDTO fromEntity(Notification n) {
        NotificationDTO dto = new NotificationDTO();
        dto.id = n.getId();
        dto.eventId = n.getEvent() != null ? n.getEvent().getId() : null;
        dto.message = n.getMessage();
        dto.read = n.isRead();
        dto.createdAt = n.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public Long getEventId() { return eventId; }
    public String getMessage() { return message; }
    public boolean isRead() { return read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
