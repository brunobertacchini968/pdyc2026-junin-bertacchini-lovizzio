package ar.edu.unnoba.pdyc2026.catalog.controller;

import ar.edu.unnoba.pdyc2026.catalog.dto.ArtistDTO;
import ar.edu.unnoba.pdyc2026.catalog.dto.EventSummaryDTO;
import ar.edu.unnoba.pdyc2026.catalog.model.Artist;
import ar.edu.unnoba.pdyc2026.catalog.model.Genre;
import ar.edu.unnoba.pdyc2026.catalog.service.ArtistService;
import ar.edu.unnoba.pdyc2026.catalog.service.EventService;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/{id}")
    public ArtistDTO getArtistById(@PathVariable Long id) {
        return ArtistDTO.fromEntity(artistService.findById(id));
    }

    @GetMapping("/{id}/events")
    public List<EventSummaryDTO> getArtistEvents(@PathVariable Long id) {
        return eventService.findUpcomingEventsForArtist(id).stream()
                .map(EventSummaryDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @PostMapping("/search/by-ids")
    public List<ArtistDTO> getArtistsByIds(@RequestBody List<Long> artistIds) {
        return artistService.findAll(null).stream()
                .filter(a -> artistIds.contains(a.getId()))
                .map(ArtistDTO::fromEntity)
                .collect(Collectors.toList());
    }
}
