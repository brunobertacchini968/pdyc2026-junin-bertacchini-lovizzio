package ar.edu.unnoba.pdyc2026.catalog.controller;

import ar.edu.unnoba.pdyc2026.catalog.dto.EventDetailDTO;
import ar.edu.unnoba.pdyc2026.catalog.dto.EventSummaryDTO;
import ar.edu.unnoba.pdyc2026.catalog.service.EventService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/events")
public class PublicEventController {

    private final EventService eventService;

    public PublicEventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public List<EventSummaryDTO> getEvents() {
        return eventService.findPublicEvents().stream()
                .map(EventSummaryDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public EventDetailDTO getEvent(@PathVariable Long id) {
        return EventDetailDTO.fromEntity(eventService.findPublicById(id));
    }

    @PostMapping("/upcoming/by-artists")
    public List<EventSummaryDTO> getUpcomingEventsForArtists(@RequestBody List<Long> artistIds) {
        return eventService.findUpcomingEventsForArtists(artistIds).stream()
                .map(EventSummaryDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @PostMapping("/search/by-ids")
    public List<EventSummaryDTO> getEventsByIds(@RequestBody List<Long> eventIds) {
        return eventService.findAll(null).stream()
                .filter(e -> eventIds.contains(e.getId()))
                .map(EventSummaryDTO::fromEntity)
                .collect(Collectors.toList());
    }
}
