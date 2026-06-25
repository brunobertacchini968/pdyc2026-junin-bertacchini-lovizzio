package ar.edu.unnoba.pdyc2026.notification.service;

import ar.edu.unnoba.pdyc2026.notification.model.Notification;
import java.util.List;

public interface NotificationService {
    List<Notification> getNotifications(String username);
    List<Notification> getUnreadNotifications(String username);
    Notification markAsRead(Long notificationId, String username);
}
