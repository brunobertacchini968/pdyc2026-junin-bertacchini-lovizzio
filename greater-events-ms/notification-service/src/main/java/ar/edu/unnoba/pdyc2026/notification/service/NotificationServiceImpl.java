package ar.edu.unnoba.pdyc2026.notification.service;

import ar.edu.unnoba.pdyc2026.notification.exception.ResourceNotFoundException;
import ar.edu.unnoba.pdyc2026.notification.model.Notification;
import ar.edu.unnoba.pdyc2026.notification.repository.NotificationRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getNotifications(String username) {
        return notificationRepository.findByUsernameOrderByCreatedAtDesc(username);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(String username) {
        return notificationRepository.findByUsernameAndReadFalseOrderByCreatedAtDesc(username);
    }

    @Override
    public Notification markAsRead(Long notificationId, String username) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        if (!n.getUsername().equals(username)) {
            throw new AccessDeniedException("You can only mark your own notifications as read");
        }
        n.setRead(true);
        return notificationRepository.save(n);
    }
}
