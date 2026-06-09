package ar.edu.unnoba.pdyc2026.events.controller;

import ar.edu.unnoba.pdyc2026.events.dto.ArtistIdDTO;
import ar.edu.unnoba.pdyc2026.events.dto.EventIdDTO;
import ar.edu.unnoba.pdyc2026.events.model.Artist;
import ar.edu.unnoba.pdyc2026.events.model.Event;
import ar.edu.unnoba.pdyc2026.events.service.UserService;
import ar.edu.unnoba.pdyc2026.events.dto.NotificationDTO;
import ar.edu.unnoba.pdyc2026.events.model.Notification;
import ar.edu.unnoba.pdyc2026.events.repository.NotificationRepository;
import ar.edu.unnoba.pdyc2026.events.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/me")
public class MeController {

    private final UserService userService;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    public MeController(UserService userService, NotificationRepository notificationRepository, NotificationService notificationService) {
        this.userService = userService;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
    }

    @PostMapping("/following")
    public ResponseEntity<Void> followArtist(Authentication auth, @Valid @RequestBody ArtistIdDTO body) {
        userService.followArtist(auth.getName(), body.getArtistId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/following/{artistId}")
    public ResponseEntity<Void> unfollowArtist(Authentication auth, @PathVariable Long artistId) {
        userService.unfollowArtist(auth.getName(), artistId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/following")
    public ResponseEntity<List<Artist>> getFollowing(Authentication auth) {
        return ResponseEntity.ok(userService.getFollowedArtists(auth.getName()));
    }

    @PostMapping("/favorite-events")
    public ResponseEntity<Void> favoriteEvent(Authentication auth, @Valid @RequestBody EventIdDTO body) {
        userService.favoriteEvent(auth.getName(), body.getEventId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/favorite-events/{eventId}")
    public ResponseEntity<Void> unfavoriteEvent(Authentication auth, @PathVariable Long eventId) {
        userService.unfavoriteEvent(auth.getName(), eventId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/favorite-events")
    public ResponseEntity<List<Event>> getFavoriteEvents(Authentication auth) {
        return ResponseEntity.ok(userService.getFavoriteEvents(auth.getName()));
    }

    @GetMapping("/following/events")
    public ResponseEntity<List<Event>> getFollowingEvents(Authentication auth) {
        return ResponseEntity.ok(userService.getFollowingEvents(auth.getName()));
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationDTO>> getNotifications(Authentication auth) {
        List<Notification> list = notificationRepository.findByUserUsernameOrderByCreatedAtDesc(auth.getName());
        List<NotificationDTO> dtos = list.stream().map(NotificationDTO::fromEntity).toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<Void> markNotificationRead(Authentication auth, @PathVariable Long id) {
        Notification n = notificationRepository.findById(id).orElseThrow();
        if (!n.getUser().getUsername().equals(auth.getName())) {
            return ResponseEntity.status(403).build();
        }
        n.setRead(true);
        notificationRepository.save(n);
        return ResponseEntity.ok().build();
    }
}
