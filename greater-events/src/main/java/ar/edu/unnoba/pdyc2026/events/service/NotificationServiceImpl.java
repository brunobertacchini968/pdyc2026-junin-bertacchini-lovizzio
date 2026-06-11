package ar.edu.unnoba.pdyc2026.events.service;

import ar.edu.unnoba.pdyc2026.events.model.Event;
import ar.edu.unnoba.pdyc2026.events.model.Notification;
import ar.edu.unnoba.pdyc2026.events.model.User;
import ar.edu.unnoba.pdyc2026.events.repository.NotificationRepository;
import ar.edu.unnoba.pdyc2026.events.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void notifyEventChange(Event event, String message) {
        Set<User> recipients = new HashSet<>();

        // users who follow any artist on the event
        event.getArtists().forEach(artist -> {
            List<User> users = userRepository.findDistinctByFollowedArtistsId(artist.getId());
            recipients.addAll(users);
        });

        // users who favorited the event
        List<User> favoriters = userRepository.findDistinctByFavoriteEventsId(event.getId());
        recipients.addAll(favoriters);

        recipients.forEach(user -> {
            Notification n = new Notification(user, event, message);
            notificationRepository.save(n);
        });
    }
}
