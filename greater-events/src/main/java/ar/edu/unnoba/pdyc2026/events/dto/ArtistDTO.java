package ar.edu.unnoba.pdyc2026.events.dto;

import ar.edu.unnoba.pdyc2026.events.model.Artist;
import ar.edu.unnoba.pdyc2026.events.model.Genre;

public class ArtistDTO {

    private Long id;
    private String name;
    private Genre genre;
    private boolean active;

    public ArtistDTO() {
    }

    public ArtistDTO(Long id, String name, Genre genre, boolean active) {
        this.id = id;
        this.name = name;
        this.genre = genre;
        this.active = active;
    }

    public static ArtistDTO fromEntity(Artist artist) {
        return new ArtistDTO(artist.getId(), artist.getName(), artist.getGenre(), artist.isActive());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Genre getGenre() {
        return genre;
    }

    public void setGenre(Genre genre) {
        this.genre = genre;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
