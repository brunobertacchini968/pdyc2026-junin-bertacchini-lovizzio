package ar.edu.unnoba.pdyc2026.events.repository;

import ar.edu.unnoba.pdyc2026.events.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserUsernameOrderByCreatedAtDesc(String username);

    List<Notification> findByUserUsernameAndReadFalseOrderByCreatedAtDesc(String username);
}
