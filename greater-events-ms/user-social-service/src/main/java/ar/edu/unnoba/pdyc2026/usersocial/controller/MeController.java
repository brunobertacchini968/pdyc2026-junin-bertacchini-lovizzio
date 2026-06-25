package ar.edu.unnoba.pdyc2026.usersocial.controller;

import ar.edu.unnoba.pdyc2026.usersocial.dto.ArtistDTO;
import ar.edu.unnoba.pdyc2026.usersocial.dto.ArtistIdDTO;
import ar.edu.unnoba.pdyc2026.usersocial.dto.EventIdDTO;
import ar.edu.unnoba.pdyc2026.usersocial.dto.EventSummaryDTO;
import ar.edu.unnoba.pdyc2026.usersocial.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/me")
public class MeController {

    private final UserService userService;

    public MeController(UserService userService) {
        this.userService = userService;
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
    public ResponseEntity<List<ArtistDTO>> getFollowing(Authentication auth) {
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
    public ResponseEntity<List<EventSummaryDTO>> getFavoriteEvents(Authentication auth) {
        return ResponseEntity.ok(userService.getFavoriteEvents(auth.getName()));
    }

    @GetMapping("/following/events")
    public ResponseEntity<List<EventSummaryDTO>> getFollowingEvents(Authentication auth) {
        return ResponseEntity.ok(userService.getFollowingEvents(auth.getName()));
    }
}
