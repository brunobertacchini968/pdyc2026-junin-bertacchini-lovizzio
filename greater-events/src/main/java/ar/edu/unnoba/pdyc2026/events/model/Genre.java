package ar.edu.unnoba.pdyc2026.events.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Genre {
    ROCK,
    TECHNO,
    POP,
    JAZZ,
    FOLK;

    @JsonCreator
    public static Genre fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Artist genre is required. Valid values: ROCK, TECHNO, POP, JAZZ, FOLK");
        }
        try {
            return Genre.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid genre: '" + value + "'. Valid values are: ROCK, TECHNO, POP, JAZZ, FOLK");
        }
    }
}
