package ar.edu.unnoba.pdyc2026.notification.controller;

import ar.edu.unnoba.pdyc2026.notification.dto.NotificationDTO;
import ar.edu.unnoba.pdyc2026.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/me/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationDTO> getNotifications(Authentication auth,
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly) {
        if (unreadOnly) {
            return notificationService.getUnreadNotifications(auth.getName())
                    .stream().map(NotificationDTO::fromEntity).collect(Collectors.toList());
        }
        return notificationService.getNotifications(auth.getName())
                .stream().map(NotificationDTO::fromEntity).collect(Collectors.toList());
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationDTO> markAsRead(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(
                NotificationDTO.fromEntity(notificationService.markAsRead(id, auth.getName())));
    }
}
