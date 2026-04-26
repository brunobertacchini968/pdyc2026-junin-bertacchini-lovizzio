package ar.edu.unnoba.pdyc2026.events.service;

import ar.edu.unnoba.pdyc2026.events.exception.BusinessException;
import ar.edu.unnoba.pdyc2026.events.exception.ResourceNotFoundException;
import ar.edu.unnoba.pdyc2026.events.model.Artist;
import ar.edu.unnoba.pdyc2026.events.model.Genre;
import ar.edu.unnoba.pdyc2026.events.repository.ArtistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ArtistServiceImpl implements ArtistService {

    private final ArtistRepository artistRepository;

    public ArtistServiceImpl(ArtistRepository artistRepository) {
        this.artistRepository = artistRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Artist> findAll(Genre genre) {
        if (genre != null) {
            return artistRepository.findByGenre(genre);
        }
        return artistRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Artist findById(Long id) {
        return artistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found with id: " + id));
    }

    @Override
    public Artist create(String name, Genre genre) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("Artist name is required");
        }
        if (genre == null) {
            throw new BusinessException("Artist genre is required");
        }
        Artist artist = new Artist(name, genre);
        return artistRepository.save(artist);
    }

    @Override
    public Artist update(Long id, String name, Genre genre) {
        Artist artist = findById(id);

        if (!artist.getEvents().isEmpty()) {
            throw new BusinessException(
                    "Cannot edit artist: it has been assigned to events. You may deactivate it instead.");
        }

        if (name != null && !name.isBlank()) {
            artist.setName(name);
        }
        if (genre != null) {
            artist.setGenre(genre);
        }
        return artistRepository.save(artist);
    }

    @Override
    public void delete(Long id) {
        Artist artist = findById(id);

        if (artist.getEvents().isEmpty()) {
            artistRepository.delete(artist);
        } else {
            // Tiene eventos asociados: no se borra, se desactiva.
            artist.setActive(false);
            artistRepository.save(artist);
        }
    }
}
