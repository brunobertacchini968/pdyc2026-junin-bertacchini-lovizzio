package ar.edu.unnoba.pdyc2026.events.controller;

import ar.edu.unnoba.pdyc2026.events.dto.ArtistDTO;
import ar.edu.unnoba.pdyc2026.events.model.Artist;
import ar.edu.unnoba.pdyc2026.events.model.Genre;
import ar.edu.unnoba.pdyc2026.events.service.ArtistService;
import ar.edu.unnoba.pdyc2026.events.service.EventService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/artists")
public class PublicArtistController {

    private final ArtistService artistService;
    private final EventService eventService;

    public PublicArtistController(ArtistService artistService, EventService eventService) {
        this.artistService = artistService;
        this.eventService = eventService;
    }

    @GetMapping
    public List<ArtistDTO> getActiveArtists(@RequestParam(required = false) String genre) {
        Genre genreFilter = genre == null ? null : Genre.fromString(genre);
        return artistService.findAll(genreFilter).stream()
                .filter(Artist::isActive)
                .map(ArtistDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}/events")
    public List<?> getArtistEvents(@PathVariable Long id) {
        return eventService.findUpcomingEventsForArtist(id).stream()
                .map(ar.edu.unnoba.pdyc2026.events.dto.EventSummaryDTO::fromEntity)
                .collect(Collectors.toList());
    }
}
