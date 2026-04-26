package ar.edu.unnoba.pdyc2026.events.service;

import ar.edu.unnoba.pdyc2026.events.model.Event;
import ar.edu.unnoba.pdyc2026.events.model.EventState;

import java.time.LocalDate;
import java.util.List;

public interface EventService {

    List<Event> findAll(EventState state);

    Event findById(Long id);

    Event create(String name, String description, LocalDate startDate);

    Event update(Long id, String name, String description, LocalDate startDate);

    void delete(Long id);

    Event addArtist(Long eventId, Long artistId);

    Event removeArtist(Long eventId, Long artistId);

    Event confirm(Long id);

    Event reschedule(Long id, LocalDate newStartDate);

    Event cancel(Long id);
}
