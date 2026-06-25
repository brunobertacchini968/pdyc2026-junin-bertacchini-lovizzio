package ar.edu.unnoba.pdyc2026.notification.repository;

import ar.edu.unnoba.pdyc2026.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUsernameOrderByCreatedAtDesc(String username);
    List<Notification> findByUsernameAndReadFalseOrderByCreatedAtDesc(String username);
}
