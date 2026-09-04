package ar.edu.unnoba.pdyc2026.usersocial.service;

import ar.edu.unnoba.pdyc2026.usersocial.dto.ArtistDTO;
import ar.edu.unnoba.pdyc2026.usersocial.dto.EventState;
import ar.edu.unnoba.pdyc2026.usersocial.dto.EventSummaryDTO;
import ar.edu.unnoba.pdyc2026.usersocial.exception.BusinessException;
import ar.edu.unnoba.pdyc2026.usersocial.exception.ResourceNotFoundException;
import ar.edu.unnoba.pdyc2026.usersocial.feign.CatalogFeignClient;
import ar.edu.unnoba.pdyc2026.usersocial.model.User;
import ar.edu.unnoba.pdyc2026.usersocial.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CatalogFeignClient catalogFeignClient;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("Debe seguir a un artista activo exitosamente")
    void testFollowArtistSuccess() {
        User user = new User("alice", "alice@example.com");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        ArtistDTO artist = new ArtistDTO();
        artist.setId(5L);
        artist.setName("Arctic Monkeys");
        artist.setActive(true);
        when(catalogFeignClient.getArtistById(5L)).thenReturn(artist);

        userService.followArtist("alice", 5L);

        assertTrue(user.getFollowedArtists().contains(5L));
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Debe fallar al seguir un artista inexistente o cuando Feign falla")
    void testFollowNonExistentArtistFails() {
        User user = new User("alice", "alice@example.com");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(catalogFeignClient.getArtistById(99L)).thenThrow(new RuntimeException("Feign 404"));

        assertThrows(ResourceNotFoundException.class, () -> userService.followArtist("alice", 99L));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe fallar al seguir a un artista desactivado")
    void testFollowDeactivatedArtistFails() {
        User user = new User("alice", "alice@example.com");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        ArtistDTO artist = new ArtistDTO();
        artist.setId(5L);
        artist.setName("Banda Separada");
        artist.setActive(false);
        when(catalogFeignClient.getArtistById(5L)).thenReturn(artist);

        assertThrows(BusinessException.class, () -> userService.followArtist("alice", 5L));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe fallar al dejar de seguir un artista que no se sigue")
    void testUnfollowNotFollowedArtistFails() {
        User user = new User("alice", "alice@example.com");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        assertThrows(BusinessException.class, () -> userService.unfollowArtist("alice", 10L));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe agregar a favoritos un evento confirmado con fecha futura")
    void testFavoriteEventSuccess() {
        User user = new User("alice", "alice@example.com");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        EventSummaryDTO event = new EventSummaryDTO();
        event.setId(100L);
        event.setName("Festival Lollapalooza");
        event.setState(EventState.CONFIRMED);
        event.setStartDate(LocalDate.now().plusDays(20));
        when(catalogFeignClient.getEventsByIds(List.of(100L))).thenReturn(List.of(event));

        userService.favoriteEvent("alice", 100L);

        assertTrue(user.getFavoriteEvents().contains(100L));
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Debe fallar al agregar a favoritos un evento en estado TENTATIVE")
    void testFavoriteTentativeEventFails() {
        User user = new User("alice", "alice@example.com");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        EventSummaryDTO event = new EventSummaryDTO();
        event.setId(100L);
        event.setName("Festival Tentativo");
        event.setState(EventState.TENTATIVE);
        event.setStartDate(LocalDate.now().plusDays(20));
        when(catalogFeignClient.getEventsByIds(List.of(100L))).thenReturn(List.of(event));

        assertThrows(BusinessException.class, () -> userService.favoriteEvent("alice", 100L));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe fallar al agregar a favoritos un evento con fecha pasada")
    void testFavoritePastEventFails() {
        User user = new User("alice", "alice@example.com");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        EventSummaryDTO event = new EventSummaryDTO();
        event.setId(100L);
        event.setName("Festival Pasado");
        event.setState(EventState.CONFIRMED);
        event.setStartDate(LocalDate.now().minusDays(2));
        when(catalogFeignClient.getEventsByIds(List.of(100L))).thenReturn(List.of(event));

        assertThrows(BusinessException.class, () -> userService.favoriteEvent("alice", 100L));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe fallar al quitar de favoritos un evento que no estaba guardado")
    void testUnfavoriteNotFavoritedEventFails() {
        User user = new User("alice", "alice@example.com");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        assertThrows(BusinessException.class, () -> userService.unfavoriteEvent("alice", 100L));
        verify(userRepository, never()).save(any());
    }
}
