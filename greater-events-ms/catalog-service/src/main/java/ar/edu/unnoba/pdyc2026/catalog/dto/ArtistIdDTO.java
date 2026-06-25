package ar.edu.unnoba.pdyc2026.catalog.dto;

import jakarta.validation.constraints.NotNull;

public class ArtistIdDTO {

    @NotNull(message = "artist_id is required")
    private Long artistId;

    public ArtistIdDTO() {
    }

    public Long getArtistId() {
        return artistId;
    }

    public void setArtistId(Long artistId) {
        this.artistId = artistId;
    }
}
