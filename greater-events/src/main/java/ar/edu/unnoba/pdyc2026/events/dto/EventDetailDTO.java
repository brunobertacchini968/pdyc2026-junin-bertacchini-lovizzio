package ar.edu.unnoba.pdyc2026.events.dto;

import ar.edu.unnoba.pdyc2026.events.model.Event;
import ar.edu.unnoba.pdyc2026.events.model.EventState;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class EventDetailDTO {

    private Long id;
    private String name;
    private String description;
    private LocalDate startDate;
    private EventState state;
    private List<ArtistDTO> artists;

    public EventDetailDTO() {
    }

    public EventDetailDTO(Long id, String name, String description, LocalDate startDate,
                          EventState state, List<ArtistDTO> artists) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.state = state;
        this.artists = artists;
    }

    public static EventDetailDTO fromEntity(Event event) {
        List<ArtistDTO> artistDTOs = event.getArtists().stream()
                .map(ArtistDTO::fromEntity)
                .collect(Collectors.toList());
        return new EventDetailDTO(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getStartDate(),
                event.getState(),
                artistDTOs
        );
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public EventState getState() {
        return state;
    }

    public void setState(EventState state) {
        this.state = state;
    }

    public List<ArtistDTO> getArtists() {
        return artists;
    }

    public void setArtists(List<ArtistDTO> artists) {
        this.artists = artists;
    }
}
