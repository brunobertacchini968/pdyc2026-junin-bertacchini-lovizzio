package ar.edu.unnoba.pdyc2026.catalog.dto;

import jakarta.validation.constraints.NotNull;

public class EventIdDTO {

    @NotNull(message = "event_id is required")
    private Long eventId;

    public EventIdDTO() {
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }
}
