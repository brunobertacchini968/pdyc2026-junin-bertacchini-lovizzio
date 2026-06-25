package ar.edu.unnoba.pdyc2026.catalog.messaging;

import java.util.List;

public class EventCancelledMessage {
    private Long eventId;
    private String eventName;
    private List<Long> artistIds;

    public EventCancelledMessage() {}

    public EventCancelledMessage(Long eventId, String eventName, List<Long> artistIds) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.artistIds = artistIds;
    }

    public Long getEventId() { return eventId; }
    public String getEventName() { return eventName; }
    public List<Long> getArtistIds() { return artistIds; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    public void setArtistIds(List<Long> artistIds) { this.artistIds = artistIds; }
}
