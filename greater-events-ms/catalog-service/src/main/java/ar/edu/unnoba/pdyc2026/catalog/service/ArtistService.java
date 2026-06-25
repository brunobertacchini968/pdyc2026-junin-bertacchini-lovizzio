package ar.edu.unnoba.pdyc2026.catalog.service;

import ar.edu.unnoba.pdyc2026.catalog.model.Artist;
import ar.edu.unnoba.pdyc2026.catalog.model.Genre;

import java.util.List;

public interface ArtistService {

    List<Artist> findAll(Genre genre);

    Artist findById(Long id);

    Artist create(String name, Genre genre);

    Artist update(Long id, String name, Genre genre);

    void delete(Long id);
}
