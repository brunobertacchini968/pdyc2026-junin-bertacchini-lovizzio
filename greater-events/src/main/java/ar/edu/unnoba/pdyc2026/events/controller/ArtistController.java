package ar.edu.unnoba.pdyc2026.events.controller;

import ar.edu.unnoba.pdyc2026.events.dto.ArtistDTO;
import ar.edu.unnoba.pdyc2026.events.dto.CreateArtistDTO;
import ar.edu.unnoba.pdyc2026.events.dto.UpdateArtistDTO;
import ar.edu.unnoba.pdyc2026.events.model.Artist;
import ar.edu.unnoba.pdyc2026.events.model.Genre;
import ar.edu.unnoba.pdyc2026.events.service.ArtistService;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/artists")
public class ArtistController {

    private final ArtistService artistService;

    public ArtistController(ArtistService artistService) {
        this.artistService = artistService;
    }

    @GetMapping
    public List<ArtistDTO> getArtists(@RequestParam(required = false) String genre) {
        Genre genreFilter = genre == null ? null : Genre.fromString(genre);
        return artistService.findAll(genreFilter).stream()
                .map(ArtistDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ArtistDTO getArtist(@PathVariable Long id) {
        return ArtistDTO.fromEntity(artistService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ArtistDTO> createArtist(@Valid @RequestBody CreateArtistDTO body) {
        Artist artist = artistService.create(body.getName(), body.getGenre());
        return ResponseEntity.status(HttpStatus.CREATED).body(ArtistDTO.fromEntity(artist));
    }

    @PutMapping("/{id}")
    public ArtistDTO updateArtist(@PathVariable Long id, @Valid @RequestBody UpdateArtistDTO body) {
        Artist artist = artistService.update(id, body.getName(), body.getGenre());
        return ArtistDTO.fromEntity(artist);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArtist(@PathVariable Long id) {
        artistService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
