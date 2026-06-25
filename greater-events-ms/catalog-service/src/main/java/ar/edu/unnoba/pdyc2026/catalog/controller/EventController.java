package ar.edu.unnoba.pdyc2026.catalog.controller;

import ar.edu.unnoba.pdyc2026.catalog.dto.*;
import ar.edu.unnoba.pdyc2026.catalog.model.Event;
import ar.edu.unnoba.pdyc2026.catalog.model.EventState;
import ar.edu.unnoba.pdyc2026.catalog.service.EventService;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public List<EventSummaryDTO> getEvents(@RequestParam(required = false) String state) {
        EventState stateFilter = state == null ? null : EventState.fromString(state);
        return eventService.findAll(stateFilter).stream()
                .map(EventSummaryDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public EventDetailDTO getEvent(@PathVariable Long id) {
        return EventDetailDTO.fromEntity(eventService.findById(id));
    }

    @PostMapping
    public ResponseEntity<EventDetailDTO> createEvent(@Valid @RequestBody CreateEventDTO body) {
        Event event = eventService.create(body.getName(), body.getDescription(), body.getStartDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(EventDetailDTO.fromEntity(event));
    }

    @PutMapping("/{id}")
    public EventDetailDTO updateEvent(@PathVariable Long id, @Valid @RequestBody UpdateEventDTO body) {
        Event event = eventService.update(id, body.getName(), body.getDescription(), body.getStartDate());
        return EventDetailDTO.fromEntity(event);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/artists")
    public EventDetailDTO addArtist(@PathVariable Long id, @Valid @RequestBody AddArtistDTO body) {
        Event event = eventService.addArtist(id, body.getArtistId());
        return EventDetailDTO.fromEntity(event);
    }

    @DeleteMapping("/{id}/artist/{artistId}")
    public EventDetailDTO removeArtist(@PathVariable Long id, @PathVariable Long artistId) {
        Event event = eventService.removeArtist(id, artistId);
        return EventDetailDTO.fromEntity(event);
    }

    @PutMapping("/{id}/confirmed")
    public EventDetailDTO confirm(@PathVariable Long id) {
        return EventDetailDTO.fromEntity(eventService.confirm(id));
    }

    @PutMapping("/{id}/rescheduled")
    public EventDetailDTO reschedule(@PathVariable Long id, @Valid @RequestBody RescheduleEventDTO body) {
        return EventDetailDTO.fromEntity(eventService.reschedule(id, body.getStartDate()));
    }

    @PutMapping("/{id}/canceled")
    public EventDetailDTO cancel(@PathVariable Long id) {
        return EventDetailDTO.fromEntity(eventService.cancel(id));
    }
}
