package ar.edu.unnoba.pdyc2026.usersocial;

import ar.edu.unnoba.pdyc2026.usersocial.dto.ArtistDTO;
import ar.edu.unnoba.pdyc2026.usersocial.dto.ArtistIdDTO;
import ar.edu.unnoba.pdyc2026.usersocial.dto.EventIdDTO;
import ar.edu.unnoba.pdyc2026.usersocial.dto.EventState;
import ar.edu.unnoba.pdyc2026.usersocial.dto.EventSummaryDTO;
import ar.edu.unnoba.pdyc2026.usersocial.dto.Genre;
import ar.edu.unnoba.pdyc2026.usersocial.feign.CatalogFeignClient;
import ar.edu.unnoba.pdyc2026.usersocial.model.User;
import ar.edu.unnoba.pdyc2026.usersocial.repository.UserRepository;
import ar.edu.unnoba.pdyc2026.usersocial.service.UserService;
import org.keycloak.admin.client.Keycloak;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class UserSocialServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private CatalogFeignClient catalogFeignClient;

    @MockBean
    private Keycloak keycloak;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        testUser = userRepository.save(new User("johndoe", "john@example.com"));
    }

    @Test
    @DisplayName("Debe permitir al usuario seguir y dejar de seguir a un artista")
    void testFollowAndUnfollowArtist() {
        ArtistDTO artistDTO = new ArtistDTO();
        artistDTO.setId(10L);
        artistDTO.setName("Charly Garcia");
        artistDTO.setGenre(Genre.ROCK);
        artistDTO.setActive(true);

        when(catalogFeignClient.getArtistById(10L)).thenReturn(artistDTO);
        when(catalogFeignClient.getArtistsByIds(List.of(10L))).thenReturn(List.of(artistDTO));

        // Seguir artista
        userService.followArtist("johndoe", 10L);

        User userAfterFollow = userRepository.findByUsername("johndoe").orElseThrow();
        assertTrue(userAfterFollow.getFollowedArtists().contains(10L));

        List<ArtistDTO> followedArtists = userService.getFollowedArtists("johndoe");
        assertEquals(1, followedArtists.size());
        assertEquals("Charly Garcia", followedArtists.get(0).getName());

        // Dejar de seguir artista
        userService.unfollowArtist("johndoe", 10L);
        User userAfterUnfollow = userRepository.findByUsername("johndoe").orElseThrow();
        assertFalse(userAfterUnfollow.getFollowedArtists().contains(10L));
    }

    @Test
    @DisplayName("Debe permitir al usuario guardar y quitar un evento de sus favoritos")
    void testFavoriteAndUnfavoriteEvent() {
        EventSummaryDTO eventDTO = new EventSummaryDTO();
        eventDTO.setId(20L);
        eventDTO.setName("Quilmes Rock");
        eventDTO.setState(EventState.CONFIRMED);
        eventDTO.setStartDate(LocalDate.now().plusDays(40));

        when(catalogFeignClient.getEventById(20L)).thenReturn(eventDTO);
        when(catalogFeignClient.getEventsByIds(List.of(20L))).thenReturn(List.of(eventDTO));

        // Agregar a favoritos
        userService.favoriteEvent("johndoe", 20L);

        User userAfterFav = userRepository.findByUsername("johndoe").orElseThrow();
        assertTrue(userAfterFav.getFavoriteEvents().contains(20L));

        List<EventSummaryDTO> favoriteEvents = userService.getFavoriteEvents("johndoe");
        assertEquals(1, favoriteEvents.size());
        assertEquals("Quilmes Rock", favoriteEvents.get(0).getName());

        // Quitar de favoritos
        userService.unfavoriteEvent("johndoe", 20L);
        User userAfterUnfav = userRepository.findByUsername("johndoe").orElseThrow();
        assertFalse(userAfterUnfav.getFavoriteEvents().contains(20L));
    }
}
