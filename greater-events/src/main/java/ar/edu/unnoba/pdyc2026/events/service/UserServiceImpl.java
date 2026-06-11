package ar.edu.unnoba.pdyc2026.events.service;

import ar.edu.unnoba.pdyc2026.events.exception.BusinessException;
import ar.edu.unnoba.pdyc2026.events.exception.ResourceNotFoundException;
import ar.edu.unnoba.pdyc2026.events.model.Artist;
import ar.edu.unnoba.pdyc2026.events.model.Event;
import ar.edu.unnoba.pdyc2026.events.model.EventState;
import ar.edu.unnoba.pdyc2026.events.model.User;
import ar.edu.unnoba.pdyc2026.events.repository.ArtistRepository;
import ar.edu.unnoba.pdyc2026.events.repository.EventRepository;
import ar.edu.unnoba.pdyc2026.events.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    private final EventRepository eventRepository;

    public UserServiceImpl(UserRepository userRepository, ArtistRepository artistRepository, EventRepository eventRepository) {
        this.userRepository = userRepository;
        this.artistRepository = artistRepository;
        this.eventRepository = eventRepository;
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    @Override
    public void followArtist(String username, Long artistId) {
        User user = findUser(username);
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found with id: " + artistId));
        if (!artist.isActive()) {
            throw new BusinessException("Cannot follow a deactivated artist");
        }
        user.getFollowedArtists().add(artist);
        userRepository.save(user);
    }

    @Override
    public void unfollowArtist(String username, Long artistId) {
        User user = findUser(username);
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found with id: " + artistId));
        if (!user.getFollowedArtists().remove(artist)) {
            throw new BusinessException("Artist is not being followed");
        }
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Artist> getFollowedArtists(String username) {
        User user = findUser(username);
        return user.getFollowedArtists().stream()
                .filter(Artist::isActive)
                .collect(Collectors.toList());
    }

    @Override
    public void favoriteEvent(String username, Long eventId) {
        User user = findUser(username);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        if (event.getState() != EventState.CONFIRMED && event.getState() != EventState.RESCHEDULED) {
            throw new BusinessException("Only confirmed or rescheduled events can be favorited");
        }
        if (event.getStartDate() == null || !event.getStartDate().isAfter(LocalDate.now())) {
            throw new BusinessException("Only future events can be favorited");
        }
        user.getFavoriteEvents().add(event);
        userRepository.save(user);
    }

    @Override
    public void unfavoriteEvent(String username, Long eventId) {
        User user = findUser(username);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        if (!user.getFavoriteEvents().remove(event)) {
            throw new BusinessException("Event is not in favorites");
        }
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Event> getFavoriteEvents(String username) {
        User user = findUser(username);
        return user.getFavoriteEvents().stream()
                .filter(e -> (e.getState() == EventState.CONFIRMED || e.getState() == EventState.RESCHEDULED) && e.getStartDate() != null && e.getStartDate().isAfter(LocalDate.now()))
                .sorted((a, b) -> a.getStartDate().compareTo(b.getStartDate()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Event> getFollowingEvents(String username) {
        User user = findUser(username);
        List<Long> artistIds = user.getFollowedArtists().stream().map(Artist::getId).collect(Collectors.toList());
        if (artistIds.isEmpty()) {
            return List.of();
        }

        var states = List.of(EventState.CONFIRMED, EventState.RESCHEDULED);
        var today = LocalDate.now();

        var events = artistIds.stream()
                .flatMap(aid -> eventRepository.findDistinctByArtistsIdAndStateInAndStartDateAfterOrderByStartDateAsc(aid, states, today).stream())
                .collect(Collectors.toMap(Event::getId, e -> e, (a, b) -> a))
                .values()
                .stream()
                .sorted((a, b) -> a.getStartDate().compareTo(b.getStartDate()))
                .collect(Collectors.toList());

        return events;
    }
}
