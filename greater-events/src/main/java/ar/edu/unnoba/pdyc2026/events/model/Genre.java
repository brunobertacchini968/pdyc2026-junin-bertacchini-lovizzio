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
        return Genre.valueOf(value.toUpperCase());
    }
}
