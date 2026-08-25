package ar.edu.unnoba.pdyc2026.catalog;

import ar.edu.unnoba.pdyc2026.catalog.exception.BusinessException;
import ar.edu.unnoba.pdyc2026.catalog.model.Artist;
import ar.edu.unnoba.pdyc2026.catalog.model.Event;
import ar.edu.unnoba.pdyc2026.catalog.model.EventState;
import ar.edu.unnoba.pdyc2026.catalog.model.Genre;
import ar.edu.unnoba.pdyc2026.catalog.service.ArtistService;
import ar.edu.unnoba.pdyc2026.catalog.service.EventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class CatalogServiceIntegrationTest {

    @Autowired
    private ArtistService artistService;

    @Autowired
    private EventService eventService;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("Debe crear un artista y buscarlo por su género")
    void testCreateAndFindArtistByGenre() {
        Artist artist = artistService.create("Soda Stereo", Genre.ROCK);
        assertNotNull(artist.getId());
        assertEquals("Soda Stereo", artist.getName());
        assertEquals(Genre.ROCK, artist.getGenre());
        assertTrue(artist.isActive());

        List<Artist> rockArtists = artistService.findAll(Genre.ROCK);
        assertFalse(rockArtists.isEmpty());
        assertTrue(rockArtists.stream().anyMatch(a -> a.getName().equals("Soda Stereo")));
    }

    @Test
    @DisplayName("Debe crear un evento en estado TENTATIVE y permitir agregar artistas")
    void testCreateEventAndAddArtist() {
        Artist artist = artistService.create("Daft Punk", Genre.TECHNO);
        Event event = eventService.create("Ultra Music Festival", "Festival Electrónico", LocalDate.now().plusDays(30));

        assertNotNull(event.getId());
        assertEquals(EventState.TENTATIVE, event.getState());

        Event updatedEvent = eventService.addArtist(event.getId(), artist.getId());
        assertEquals(1, updatedEvent.getArtists().size());
        assertTrue(updatedEvent.getArtists().stream().anyMatch(a -> a.getId().equals(artist.getId())));
    }

    @Test
    @DisplayName("Debe cambiar el estado del evento a CONFIRMED y luego CANCELLED notificando a RabbitMQ")
    void testConfirmAndCancelEventFlow() {
        Event event = eventService.create("Lollapalooza 2027", "Festival Anual", LocalDate.now().plusDays(60));

        Event confirmedEvent = eventService.confirm(event.getId());
        assertEquals(EventState.CONFIRMED, confirmedEvent.getState());

        Event cancelledEvent = eventService.cancel(confirmedEvent.getId());
        assertEquals(EventState.CANCELLED, cancelledEvent.getState());

        // Verificamos que se haya enviado el mensaje de cancelación a RabbitMQ
        verify(rabbitTemplate).convertAndSend(eq("events.exchange"), eq("event.cancelled"), any(Object.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción al intentar editar un evento en estado CONFIRMED")
    void testFailUpdateNonTentativeEvent() {
        Event event = eventService.create("Concierto de Jazz", "Recital en vivo", LocalDate.now().plusDays(15));
        eventService.confirm(event.getId());

        assertThrows(BusinessException.class, () ->
                eventService.update(event.getId(), "Nuevo Nombre", "Nueva desc", LocalDate.now().plusDays(20))
        );
    }

    @Test
    @DisplayName("Debe desactivar el artista en lugar de eliminarlo si tiene eventos asociados")
    void testDeactivateArtistWithEvents() {
        Artist artist = artistService.create("Astor Piazzolla", Genre.JAZZ);
        Event event = eventService.create("Tango Fest", "Noche de Tango", LocalDate.now().plusDays(10));
        eventService.addArtist(event.getId(), artist.getId());

        artistService.delete(artist.getId());

        Artist retrieved = artistService.findById(artist.getId());
        assertFalse(retrieved.isActive(), "El artista debe estar desactivado (active=false)");
    }
}
