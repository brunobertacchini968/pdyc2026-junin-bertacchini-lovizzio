package ar.edu.unnoba.pdyc2026.events.dto;

import ar.edu.unnoba.pdyc2026.events.model.Event;
import ar.edu.unnoba.pdyc2026.events.model.EventState;

import java.time.LocalDate;

public class EventSummaryDTO {

    private Long id;
    private String name;
    private LocalDate startDate;
    private EventState state;
    private int artistsCount;

    public EventSummaryDTO() {
    }

    public EventSummaryDTO(Long id, String name, LocalDate startDate, EventState state, int artistsCount) {
        this.id = id;
        this.name = name;
        this.startDate = startDate;
        this.state = state;
        this.artistsCount = artistsCount;
    }

    public static EventSummaryDTO fromEntity(Event event) {
        return new EventSummaryDTO(
                event.getId(),
                event.getName(),
                event.getStartDate(),
                event.getState(),
                event.getArtists() == null ? 0 : event.getArtists().size()
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

    public int getArtistsCount() {
        return artistsCount;
    }

    public void setArtistsCount(int artistsCount) {
        this.artistsCount = artistsCount;
    }
}
