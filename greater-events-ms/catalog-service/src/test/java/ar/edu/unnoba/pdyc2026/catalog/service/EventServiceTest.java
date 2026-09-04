package ar.edu.unnoba.pdyc2026.catalog.service;

import ar.edu.unnoba.pdyc2026.catalog.exception.BusinessException;
import ar.edu.unnoba.pdyc2026.catalog.exception.ResourceNotFoundException;
import ar.edu.unnoba.pdyc2026.catalog.messaging.EventCancelledMessage;
import ar.edu.unnoba.pdyc2026.catalog.model.Artist;
import ar.edu.unnoba.pdyc2026.catalog.model.Event;
import ar.edu.unnoba.pdyc2026.catalog.model.EventState;
import ar.edu.unnoba.pdyc2026.catalog.model.Genre;
import ar.edu.unnoba.pdyc2026.catalog.repository.ArtistRepository;
import ar.edu.unnoba.pdyc2026.catalog.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private EventServiceImpl eventService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(eventService, "exchange", "events.exchange");
        ReflectionTestUtils.setField(eventService, "routingKey", "event.cancelled");
    }

    @Test
    @DisplayName("Debe crear un evento válidamente en estado TENTATIVE")
    void testCreateEventSuccess() {
        Event event = new Event("Rock Fest", "Festival", LocalDate.now().plusDays(20));
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        Event created = eventService.create("Rock Fest", "Festival", LocalDate.now().plusDays(20));

        assertNotNull(created);
        assertEquals(EventState.TENTATIVE, created.getState());
        assertEquals("Rock Fest", created.getName());
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    @DisplayName("Debe fallar al crear un evento con nombre vacío o fecha nula")
    void testCreateEventInvalidData() {
        assertThrows(BusinessException.class, () ->
                eventService.create("  ", "Desc", LocalDate.now().plusDays(10)));
        assertThrows(BusinessException.class, () ->
                eventService.create("Rock Fest", "Desc", null));
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe fallar al actualizar un evento que no está en estado TENTATIVE")
    void testUpdateNonTentativeEventFails() {
        Event event = new Event("Rock Fest", "Festival", LocalDate.now().plusDays(20));
        event.setId(1L);
        event.setState(EventState.CONFIRMED);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(BusinessException.class, () ->
                eventService.update(1L, "Nuevo Nombre", "Nueva Desc", LocalDate.now().plusDays(25)));
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe fallar al eliminar un evento que no está en estado TENTATIVE")
    void testDeleteNonTentativeEventFails() {
        Event event = new Event("Rock Fest", "Festival", LocalDate.now().plusDays(20));
        event.setId(1L);
        event.setState(EventState.CONFIRMED);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(BusinessException.class, () -> eventService.delete(1L));
        verify(eventRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Debe fallar al agregar un artista desactivado a un evento")
    void testAddDeactivatedArtistFails() {
        Event event = new Event("Rock Fest", "Festival", LocalDate.now().plusDays(20));
        event.setId(1L);
        Artist inactiveArtist = new Artist("Banda", Genre.ROCK);
        inactiveArtist.setId(10L);
        inactiveArtist.setActive(false);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(artistRepository.findById(10L)).thenReturn(Optional.of(inactiveArtist));

        assertThrows(BusinessException.class, () -> eventService.addArtist(1L, 10L));
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe fallar al remover un artista que no está en el evento")
    void testRemoveArtistNotInEventFails() {
        Event event = new Event("Rock Fest", "Festival", LocalDate.now().plusDays(20));
        event.setId(1L);
        Artist artist = new Artist("Banda", Genre.ROCK);
        artist.setId(10L);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(artistRepository.findById(10L)).thenReturn(Optional.of(artist));

        assertThrows(BusinessException.class, () -> eventService.removeArtist(1L, 10L));
    }

    @Test
    @DisplayName("Debe confirmar un evento con fecha futura y fallar si la fecha es pasada")
    void testConfirmEvent() {
        Event futureEvent = new Event("Futuro", "Desc", LocalDate.now().plusDays(5));
        futureEvent.setId(1L);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(futureEvent));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event confirmed = eventService.confirm(1L);
        assertEquals(EventState.CONFIRMED, confirmed.getState());

        Event pastEvent = new Event("Pasado", "Desc", LocalDate.now().minusDays(1));
        pastEvent.setId(2L);
        when(eventRepository.findById(2L)).thenReturn(Optional.of(pastEvent));

        assertThrows(BusinessException.class, () -> eventService.confirm(2L));
    }

    @Test
    @DisplayName("Debe reprogramar un evento confirmado con nueva fecha futura")
    void testRescheduleEventSuccess() {
        Event event = new Event("Recital", "Desc", LocalDate.now().plusDays(10));
        event.setId(1L);
        event.setState(EventState.CONFIRMED);

        LocalDate newDate = LocalDate.now().plusDays(30);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event rescheduled = eventService.reschedule(1L, newDate);

        assertEquals(EventState.RESCHEDULED, rescheduled.getState());
        assertEquals(newDate, rescheduled.getStartDate());
    }

    @Test
    @DisplayName("Debe fallar al reprogramar un evento con fecha pasada o no futura")
    void testRescheduleEventInvalidDate() {
        Event event = new Event("Recital", "Desc", LocalDate.now().plusDays(10));
        event.setId(1L);
        event.setState(EventState.CONFIRMED);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(BusinessException.class, () ->
                eventService.reschedule(1L, LocalDate.now().minusDays(1)));
        assertThrows(BusinessException.class, () ->
                eventService.reschedule(1L, null));
    }

    @Test
    @DisplayName("Debe cancelar evento confirmado y publicar mensaje en RabbitMQ")
    void testCancelEventSuccess() {
        Event event = new Event("Recital", "Desc", LocalDate.now().plusDays(10));
        event.setId(1L);
        event.setState(EventState.CONFIRMED);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event cancelled = eventService.cancel(1L);

        assertEquals(EventState.CANCELLED, cancelled.getState());
        verify(rabbitTemplate).convertAndSend(eq("events.exchange"), eq("event.cancelled"), any(EventCancelledMessage.class));
    }

    @Test
    @DisplayName("Debe fallar al cancelar un evento en estado TENTATIVE")
    void testCancelTentativeEventFails() {
        Event event = new Event("Recital", "Desc", LocalDate.now().plusDays(10));
        event.setId(1L);
        event.setState(EventState.TENTATIVE);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(BusinessException.class, () -> eventService.cancel(1L));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }
}
