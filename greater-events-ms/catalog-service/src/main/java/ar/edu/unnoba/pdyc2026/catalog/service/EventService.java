package ar.edu.unnoba.pdyc2026.catalog.service;

import ar.edu.unnoba.pdyc2026.catalog.model.Event;
import ar.edu.unnoba.pdyc2026.catalog.model.EventState;

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

    List<Event> findPublicEvents();

    Event findPublicById(Long id);

    List<Event> findUpcomingEventsForArtist(Long artistId);

    List<Event> findUpcomingEventsForArtists(List<Long> artistIds);
}
