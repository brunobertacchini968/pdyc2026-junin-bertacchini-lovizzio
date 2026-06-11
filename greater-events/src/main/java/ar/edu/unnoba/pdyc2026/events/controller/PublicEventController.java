package ar.edu.unnoba.pdyc2026.events.controller;

import ar.edu.unnoba.pdyc2026.events.dto.EventDetailDTO;
import ar.edu.unnoba.pdyc2026.events.dto.EventSummaryDTO;
import ar.edu.unnoba.pdyc2026.events.service.EventService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
