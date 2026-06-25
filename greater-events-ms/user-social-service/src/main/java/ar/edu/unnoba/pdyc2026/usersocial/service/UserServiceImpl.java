package ar.edu.unnoba.pdyc2026.usersocial.service;

import ar.edu.unnoba.pdyc2026.usersocial.dto.ArtistDTO;
import ar.edu.unnoba.pdyc2026.usersocial.dto.EventState;
import ar.edu.unnoba.pdyc2026.usersocial.dto.EventSummaryDTO;
import ar.edu.unnoba.pdyc2026.usersocial.exception.BusinessException;
import ar.edu.unnoba.pdyc2026.usersocial.exception.ResourceNotFoundException;
import ar.edu.unnoba.pdyc2026.usersocial.feign.CatalogFeignClient;
import ar.edu.unnoba.pdyc2026.usersocial.model.User;
import ar.edu.unnoba.pdyc2026.usersocial.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CatalogFeignClient catalogFeignClient;

    public UserServiceImpl(UserRepository userRepository, CatalogFeignClient catalogFeignClient) {
        this.userRepository = userRepository;
        this.catalogFeignClient = catalogFeignClient;
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    @Override
    public void followArtist(String username, Long artistId) {
        User user = findUser(username);

        ArtistDTO artist;
        try {
            artist = catalogFeignClient.getArtistById(artistId);
        } catch (Exception e) {
            throw new ResourceNotFoundException("Artist not found with id: " + artistId);
        }

        if (artist == null) {
            throw new ResourceNotFoundException("Artist not found with id: " + artistId);
        }
        if (!artist.isActive()) {
            throw new BusinessException("Cannot follow a deactivated artist");
        }

        user.getFollowedArtists().add(artistId);
        userRepository.save(user);
    }

    @Override
    public void unfollowArtist(String username, Long artistId) {
        User user = findUser(username);
        if (!user.getFollowedArtists().remove(artistId)) {
            throw new BusinessException("Artist is not being followed");
        }
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArtistDTO> getFollowedArtists(String username) {
        User user = findUser(username);
        if (user.getFollowedArtists().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return catalogFeignClient.getArtistsByIds(new ArrayList<>(user.getFollowedArtists())).stream()
                    .filter(ArtistDTO::isActive)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new BusinessException("Failed to retrieve followed artists: " + e.getMessage());
        }
    }

    @Override
    public void favoriteEvent(String username, Long eventId) {
        User user = findUser(username);

        List<EventSummaryDTO> events;
        try {
            events = catalogFeignClient.getEventsByIds(List.of(eventId));
        } catch (Exception e) {
            throw new ResourceNotFoundException("Event not found with id: " + eventId);
        }

        if (events == null || events.isEmpty()) {
            throw new ResourceNotFoundException("Event not found with id: " + eventId);
        }

        EventSummaryDTO event = events.get(0);
        if (event.getState() != EventState.CONFIRMED && event.getState() != EventState.RESCHEDULED) {
            throw new BusinessException("Only confirmed or rescheduled events can be favorited");
        }
        if (event.getStartDate() == null || !event.getStartDate().isAfter(LocalDate.now())) {
            throw new BusinessException("Only future events can be favorited");
        }

        user.getFavoriteEvents().add(eventId);
        userRepository.save(user);
    }

    @Override
    public void unfavoriteEvent(String username, Long eventId) {
        User user = findUser(username);
        if (!user.getFavoriteEvents().remove(eventId)) {
            throw new BusinessException("Event is not in favorites");
        }
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventSummaryDTO> getFavoriteEvents(String username) {
        User user = findUser(username);
        if (user.getFavoriteEvents().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return catalogFeignClient.getEventsByIds(new ArrayList<>(user.getFavoriteEvents())).stream()
                    .filter(e -> (e.getState() == EventState.CONFIRMED || e.getState() == EventState.RESCHEDULED) && e.getStartDate() != null && e.getStartDate().isAfter(LocalDate.now()))
                    .sorted((a, b) -> a.getStartDate().compareTo(b.getStartDate()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new BusinessException("Failed to retrieve favorite events: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventSummaryDTO> getFollowingEvents(String username) {
        User user = findUser(username);
        if (user.getFollowedArtists().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return catalogFeignClient.getUpcomingEventsForArtists(new ArrayList<>(user.getFollowedArtists())).stream()
                    .sorted((a, b) -> a.getStartDate().compareTo(b.getStartDate()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new BusinessException("Failed to retrieve following events: " + e.getMessage());
        }
    }
}
