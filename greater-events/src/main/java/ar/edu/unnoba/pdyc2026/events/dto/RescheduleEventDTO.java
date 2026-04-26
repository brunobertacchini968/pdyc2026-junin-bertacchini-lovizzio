package ar.edu.unnoba.pdyc2026.events.dto;

import java.time.LocalDate;

public class RescheduleEventDTO {

    private LocalDate startDate;

    public RescheduleEventDTO() {
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
}
