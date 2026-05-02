package ar.edu.unnoba.pdyc2026.events.controller;

import ar.edu.unnoba.pdyc2026.events.exception.BusinessException;
import ar.edu.unnoba.pdyc2026.events.exception.GlobalExceptionHandler;
import ar.edu.unnoba.pdyc2026.events.exception.ResourceNotFoundException;
import ar.edu.unnoba.pdyc2026.events.model.Artist;
import ar.edu.unnoba.pdyc2026.events.model.Event;
import ar.edu.unnoba.pdyc2026.events.model.EventState;
import ar.edu.unnoba.pdyc2026.events.model.Genre;
import ar.edu.unnoba.pdyc2026.events.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventController.class)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = "spring.jackson.property-naming-strategy=SNAKE_CASE")
class EventControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @Test
    void getEvents_returnsSummaries() throws Exception {
        Event event = event(1L, "Festival", "Descripción", LocalDate.of(2026, 6, 12), EventState.TENTATIVE);
        given(eventService.findAll(null)).willReturn(List.of(event));

        mockMvc.perform(get("/admin/events").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Festival"))
                .andExpect(jsonPath("$[0].start_date").value("2026-06-12"))
                .andExpect(jsonPath("$[0].state").value("TENTATIVE"))
                .andExpect(jsonPath("$[0].artists_count").value(0));
    }

    @Test
    void getEvents_filtersByState() throws Exception {
        Event event = event(2L, "Encuentro", "Live", LocalDate.of(2026, 6, 20), EventState.CONFIRMED);
        given(eventService.findAll(EventState.CONFIRMED)).willReturn(List.of(event));

        mockMvc.perform(get("/admin/events")
                        .param("state", "confirmed")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2L))
                .andExpect(jsonPath("$[0].state").value("CONFIRMED"));

        verify(eventService).findAll(EventState.CONFIRMED);
    }

    @Test
    void getEvents_invalidState_returns400() throws Exception {
        mockMvc.perform(get("/admin/events")
                        .param("state", "unknown")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Invalid state")));
    }

    @Test
    void getEvent_returnsDetail() throws Exception {
        Artist artist = artist(7L, "Tina", Genre.JAZZ, true);
        Event event = event(5L, "Noche", "Concierto", LocalDate.of(2026, 7, 1), EventState.CONFIRMED, artist);
        given(eventService.findById(5L)).willReturn(event);

        mockMvc.perform(get("/admin/events/5").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.name").value("Noche"))
                .andExpect(jsonPath("$.description").value("Concierto"))
                .andExpect(jsonPath("$.start_date").value("2026-07-01"))
                .andExpect(jsonPath("$.state").value("CONFIRMED"))
                .andExpect(jsonPath("$.artists[0].id").value(7L))
                .andExpect(jsonPath("$.artists[0].name").value("Tina"))
                .andExpect(jsonPath("$.artists[0].genre").value("JAZZ"))
                .andExpect(jsonPath("$.artists[0].active").value(true));
    }

    @Test
    void getEvent_notFound_returns404() throws Exception {
        given(eventService.findById(9L)).willThrow(new ResourceNotFoundException("Event not found with id: 9"));

        mockMvc.perform(get("/admin/events/9").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Event not found with id: 9"));
    }

    @Test
    void getEvent_invalidIdType_returns400() throws Exception {
        mockMvc.perform(get("/admin/events/abc").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid value for parameter 'id'"));
    }

    @Test
    void createEvent_valid_returns201() throws Exception {
        Event created = event(11L, "Expo", "Nueva edición", LocalDate.of(2026, 8, 15), EventState.TENTATIVE);
        given(eventService.create("Expo", "Nueva edición", LocalDate.of(2026, 8, 15))).willReturn(created);

        mockMvc.perform(post("/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Expo","description":"Nueva edición","start_date":"2026-08-15"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11L))
                .andExpect(jsonPath("$.name").value("Expo"))
                .andExpect(jsonPath("$.description").value("Nueva edición"))
                .andExpect(jsonPath("$.start_date").value("2026-08-15"))
                .andExpect(jsonPath("$.state").value("TENTATIVE"))
                .andExpect(jsonPath("$.artists").isArray());
    }

    @Test
    void createEvent_missingStartDate_returns400() throws Exception {
        mockMvc.perform(post("/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Expo","description":"Nueva edición"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("start_date")));
    }

    @Test
    void createEvent_invalidDateFormat_returns400() throws Exception {
        mockMvc.perform(post("/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Expo","description":"Nueva edición","start_date":"15-08-2026"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void updateEvent_valid_returns200() throws Exception {
        Event updated = event(20L, "Expo renovada", "Nueva edición", LocalDate.of(2026, 9, 1), EventState.TENTATIVE);
        given(eventService.update(20L, "Expo renovada", "Nueva edición", LocalDate.of(2026, 9, 1))).willReturn(updated);

        mockMvc.perform(put("/admin/events/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Expo renovada","description":"Nueva edición","start_date":"2026-09-01"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20L))
                .andExpect(jsonPath("$.name").value("Expo renovada"))
                .andExpect(jsonPath("$.description").value("Nueva edición"))
                .andExpect(jsonPath("$.start_date").value("2026-09-01"));
    }

    @Test
    void updateEvent_noFields_returns400() throws Exception {
        given(eventService.update(20L, null, null, null))
                .willThrow(new BusinessException("At least one field (name, description or start_date) must be provided to update event"));

        mockMvc.perform(put("/admin/events/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("At least one field (name, description or start_date) must be provided to update event"));
    }

    @Test
    void deleteEvent_returnsNoContent() throws Exception {
        willDoNothing().given(eventService).delete(30L);

        mockMvc.perform(delete("/admin/events/30"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void deleteEvent_invalidState_returns400() throws Exception {
        doThrow(new BusinessException("Only events in TENTATIVE state can be deleted"))
                .when(eventService)
                .delete(30L);

        mockMvc.perform(delete("/admin/events/30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Only events in TENTATIVE state can be deleted"));
    }

    @Test
    void addArtist_valid_returns200() throws Exception {
        Artist artist = artist(44L, "Invitada", Genre.POP, true);
        Event event = event(40L, "Show", "Con artista", LocalDate.of(2026, 10, 10), EventState.TENTATIVE, artist);
        given(eventService.addArtist(40L, 44L)).willReturn(event);

        mockMvc.perform(post("/admin/events/40/artists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"artist_id":44}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(40L))
                .andExpect(jsonPath("$.artists[0].id").value(44L))
                .andExpect(jsonPath("$.artists[0].genre").value("POP"));
    }

    @Test
    void addArtist_notFound_returns404() throws Exception {
        given(eventService.addArtist(40L, 99L))
                .willThrow(new ResourceNotFoundException("Artist not found with id: 99"));

        mockMvc.perform(post("/admin/events/40/artists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"artist_id":99}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Artist not found with id: 99"));
    }

    @Test
    void addArtist_deactivatedArtist_returns400() throws Exception {
        given(eventService.addArtist(40L, 99L))
                .willThrow(new BusinessException("Cannot add a deactivated artist to an event"));

        mockMvc.perform(post("/admin/events/40/artists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"artist_id":99}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot add a deactivated artist to an event"));
    }

    @Test
    void removeArtist_notPartOfEvent_returns400() throws Exception {
        given(eventService.removeArtist(40L, 77L))
                .willThrow(new BusinessException("Artist is not part of this event"));

        mockMvc.perform(delete("/admin/events/40/artist/77"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Artist is not part of this event"));
    }

    @Test
    void confirm_valid_returns200() throws Exception {
        Event event = event(50L, "Concierto", "Próximo", LocalDate.of(2026, 11, 5), EventState.CONFIRMED);
        given(eventService.confirm(50L)).willReturn(event);

        mockMvc.perform(put("/admin/events/50/confirmed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50L))
                .andExpect(jsonPath("$.state").value("CONFIRMED"));
    }

    @Test
    void confirm_pastEvent_returns400() throws Exception {
        given(eventService.confirm(50L))
                .willThrow(new BusinessException("Only events with a future start_date can be confirmed"));

        mockMvc.perform(put("/admin/events/50/confirmed"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Only events with a future start_date can be confirmed"));
    }

    @Test
    void reschedule_valid_returns200() throws Exception {
        Event event = event(60L, "Reprogramado", "Nueva fecha", LocalDate.of(2026, 12, 20), EventState.RESCHEDULED);
        given(eventService.reschedule(60L, LocalDate.of(2026, 12, 20))).willReturn(event);

        mockMvc.perform(put("/admin/events/60/rescheduled")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"start_date":"2026-12-20"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(60L))
                .andExpect(jsonPath("$.start_date").value("2026-12-20"))
                .andExpect(jsonPath("$.state").value("RESCHEDULED"));
    }

    @Test
    void reschedule_invalidDate_returns400() throws Exception {
        given(eventService.reschedule(60L, LocalDate.of(2026, 1, 1)))
                .willThrow(new BusinessException("New start_date must be a future date"));

        mockMvc.perform(put("/admin/events/60/rescheduled")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"start_date":"2026-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("New start_date must be a future date"));
    }

    @Test
    void cancel_valid_returns200() throws Exception {
        Event event = event(70L, "Cancelado", "Estado final", LocalDate.of(2026, 7, 30), EventState.CANCELLED);
        given(eventService.cancel(70L)).willReturn(event);

        mockMvc.perform(put("/admin/events/70/canceled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(70L))
                .andExpect(jsonPath("$.state").value("CANCELLED"));
    }

    @Test
    void cancel_invalidState_returns400() throws Exception {
        given(eventService.cancel(70L))
                .willThrow(new BusinessException("Only CONFIRMED or RESCHEDULED events can be cancelled"));

        mockMvc.perform(put("/admin/events/70/canceled"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Only CONFIRMED or RESCHEDULED events can be cancelled"));
    }

    private Event event(Long id, String name, String description, LocalDate startDate, EventState state, Artist... artists) {
        Event event = new Event(name, description, startDate);
        event.setId(id);
        event.setState(state);
        Set<Artist> artistSet = new HashSet<>(List.of(artists));
        event.setArtists(artistSet);
        return event;
    }

    private Artist artist(Long id, String name, Genre genre, boolean active) {
        Artist artist = new Artist(name, genre);
        artist.setId(id);
        artist.setActive(active);
        return artist;
    }
}


