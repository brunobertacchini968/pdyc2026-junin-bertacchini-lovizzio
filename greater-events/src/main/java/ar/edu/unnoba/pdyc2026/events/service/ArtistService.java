package ar.edu.unnoba.pdyc2026.events.service;

import ar.edu.unnoba.pdyc2026.events.model.Artist;
import ar.edu.unnoba.pdyc2026.events.model.Genre;

import java.util.List;

public interface ArtistService {

    List<Artist> findAll(Genre genre);

    Artist findById(Long id);

    Artist create(String name, Genre genre);

    Artist update(Long id, String name, Genre genre);

    void delete(Long id);
}
