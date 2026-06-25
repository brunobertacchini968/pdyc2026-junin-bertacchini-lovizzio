package ar.edu.unnoba.pdyc2026.catalog.service;

import ar.edu.unnoba.pdyc2026.catalog.exception.BusinessException;
import ar.edu.unnoba.pdyc2026.catalog.exception.ResourceNotFoundException;
import ar.edu.unnoba.pdyc2026.catalog.model.Artist;
import ar.edu.unnoba.pdyc2026.catalog.model.Event;
import ar.edu.unnoba.pdyc2026.catalog.model.EventState;
import ar.edu.unnoba.pdyc2026.catalog.repository.ArtistRepository;
import ar.edu.unnoba.pdyc2026.catalog.repository.EventRepository;
import ar.edu.unnoba.pdyc2026.catalog.messaging.EventCancelledMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final ArtistRepository artistRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key}")
    private String routingKey;

    public EventServiceImpl(EventRepository eventRepository, ArtistRepository artistRepository, RabbitTemplate rabbitTemplate) {
        this.eventRepository = eventRepository;
        this.artistRepository = artistRepository;
        this.rabbitTemplate = rabbitTemplate;
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
        Event saved = eventRepository.save(event);

        // Send RabbitMQ Event Cancelled Message
        List<Long> artistIds = saved.getArtists().stream()
                .map(Artist::getId)
                .toList();
        EventCancelledMessage message = new EventCancelledMessage(saved.getId(), saved.getName(), artistIds);
        rabbitTemplate.convertAndSend(exchange, routingKey, message);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Event> findPublicEvents() {
        var states = List.of(EventState.CONFIRMED, EventState.RESCHEDULED);
        return eventRepository.findAll().stream()
                .filter(e -> states.contains(e.getState()) && e.getStartDate() != null && e.getStartDate().isAfter(LocalDate.now()))
                .sorted((a, b) -> a.getStartDate().compareTo(b.getStartDate()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Event findPublicById(Long id) {
        Event e = findById(id);
        if (e.getState() != EventState.CONFIRMED && e.getState() != EventState.RESCHEDULED) {
            throw new ResourceNotFoundException("Event not found");
        }
        return e;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Event> findUpcomingEventsForArtist(Long artistId) {
        return eventRepository.findDistinctByArtistsIdAndStateInAndStartDateAfterOrderByStartDateAsc(
                artistId,
                List.of(EventState.CONFIRMED, EventState.RESCHEDULED),
                LocalDate.now()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Event> findUpcomingEventsForArtists(List<Long> artistIds) {
        if (artistIds == null || artistIds.isEmpty()) {
            return List.of();
        }
        return eventRepository.findDistinctByArtistsIdInAndStateInAndStartDateAfterOrderByStartDateAsc(
                artistIds,
                List.of(EventState.CONFIRMED, EventState.RESCHEDULED),
                LocalDate.now()
        );
    }
}
