package ar.edu.unnoba.pdyc2026.events.dto;

import java.time.LocalDate;

public class UpdateEventDTO {

    private String name;
    private LocalDate startDate;
    private String description;

    public UpdateEventDTO() {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
