package ar.edu.unnoba.pdyc2026.events.service;

import ar.edu.unnoba.pdyc2026.events.exception.BusinessException;
import ar.edu.unnoba.pdyc2026.events.exception.ResourceNotFoundException;
import ar.edu.unnoba.pdyc2026.events.model.Artist;
import ar.edu.unnoba.pdyc2026.events.model.Event;
import ar.edu.unnoba.pdyc2026.events.model.EventState;
import ar.edu.unnoba.pdyc2026.events.repository.ArtistRepository;
import ar.edu.unnoba.pdyc2026.events.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final ArtistRepository artistRepository;

    public EventServiceImpl(EventRepository eventRepository, ArtistRepository artistRepository) {
        this.eventRepository = eventRepository;
        this.artistRepository = artistRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Event> findAll(EventState state) {
        if (state != null) {
            return eventRepository.findByState(state);
        }
        return eventRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Event findById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + id));
    }

    @Override
    public Event create(String name, String description, LocalDate startDate) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("Event name is required");
        }
        if (startDate == null) {
            throw new BusinessException("Event start_date is required");
        }
        Event event = new Event(name, description, startDate);
        return eventRepository.save(event);
    }

    @Override
    public Event update(Long id, String name, String description, LocalDate startDate) {
        Event event = findById(id);

        if (event.getState() != EventState.TENTATIVE) {
            throw new BusinessException("Only events in TENTATIVE state can be modified");
        }

        boolean hasValidNameUpdate = name != null && !name.isBlank();
        boolean hasDescriptionUpdate = description != null;
        boolean hasStartDateUpdate = startDate != null;
        if (!hasValidNameUpdate && !hasDescriptionUpdate && !hasStartDateUpdate) {
            throw new BusinessException("At least one field (name, description or start_date) must be provided to update event");
        }
        if (name != null && name.isBlank()) {
            throw new BusinessException("Event name cannot be blank");
        }

        if (name != null && !name.isBlank()) {
            event.setName(name);
        }
        if (description != null) {
            event.setDescription(description);
        }
        if (startDate != null) {
            event.setStartDate(startDate);
        }
        return eventRepository.save(event);
    }

    @Override
    public void delete(Long id) {
        Event event = findById(id);
        if (event.getState() != EventState.TENTATIVE) {
            throw new BusinessException("Only events in TENTATIVE state can be deleted");
        }
        eventRepository.delete(event);
    }

    @Override
    public Event addArtist(Long eventId, Long artistId) {
        Event event = findById(eventId);
        if (event.getState() != EventState.TENTATIVE) {
            throw new BusinessException("Artists can only be added to events in TENTATIVE state");
        }

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found with id: " + artistId));

        if (!artist.isActive()) {
            throw new BusinessException("Cannot add a deactivated artist to an event");
        }

        event.getArtists().add(artist);
        return eventRepository.save(event);
    }

    @Override
    public Event removeArtist(Long eventId, Long artistId) {
        Event event = findById(eventId);
        if (event.getState() != EventState.TENTATIVE) {
            throw new BusinessException("Artists can only be removed from events in TENTATIVE state");
        }

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found with id: " + artistId));

        if (!event.getArtists().remove(artist)) {
            throw new BusinessException("Artist is not part of this event");
        }
        return eventRepository.save(event);
    }

    @Override
    public Event confirm(Long id) {
        Event event = findById(id);

        if (event.getState() != EventState.TENTATIVE) {
            throw new BusinessException("Only events in TENTATIVE state can be confirmed");
        }
        if (!event.getStartDate().isAfter(LocalDate.now())) {
            throw new BusinessException("Only events with a future start_date can be confirmed");
        }
        event.setState(EventState.CONFIRMED);
        return eventRepository.save(event);
    }

    @Override
    public Event reschedule(Long id, LocalDate newStartDate) {
        Event event = findById(id);

        if (event.getState() != EventState.CONFIRMED && event.getState() != EventState.RESCHEDULED) {
            throw new BusinessException("Only CONFIRMED or RESCHEDULED events can be rescheduled");
        }
        if (event.getStartDate().isBefore(LocalDate.now())) {
            throw new BusinessException("Cannot reschedule an event that has already taken place");
        }
        if (newStartDate == null || !newStartDate.isAfter(LocalDate.now())) {
            throw new BusinessException("New start_date must be a future date");
        }

        event.setStartDate(newStartDate);
        event.setState(EventState.RESCHEDULED);
        return eventRepository.save(event);
    }

    @Override
    public Event cancel(Long id) {
        Event event = findById(id);

        if (event.getState() != EventState.CONFIRMED && event.getState() != EventState.RESCHEDULED) {
            throw new BusinessException("Only CONFIRMED or RESCHEDULED events can be cancelled");
        }
        event.setState(EventState.CANCELLED);
        return eventRepository.save(event);
    }
}
