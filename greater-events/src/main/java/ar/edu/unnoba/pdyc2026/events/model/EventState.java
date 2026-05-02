package ar.edu.unnoba.pdyc2026.events.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EventState {
    TENTATIVE,
    CONFIRMED,
    RESCHEDULED,
    CANCELLED;

    @JsonCreator
    public static EventState fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Event state is required. Valid values: TENTATIVE, CONFIRMED, RESCHEDULED, CANCELLED");
        }
        try {
            return EventState.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid state: '" + value + "'. Valid values are: TENTATIVE, CONFIRMED, RESCHEDULED, CANCELLED");
        }
    }
}
