package ar.edu.unnoba.pdyc2026.notification.service;

import ar.edu.unnoba.pdyc2026.notification.exception.ResourceNotFoundException;
import ar.edu.unnoba.pdyc2026.notification.model.Notification;
import ar.edu.unnoba.pdyc2026.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    @DisplayName("Debe listar notificaciones y notificaciones no leídas")
    void testGetNotifications() {
        Notification n1 = new Notification("carol", 10L, "Mensaje 1");
        Notification n2 = new Notification("carol", 20L, "Mensaje 2");

        when(notificationRepository.findByUsernameOrderByCreatedAtDesc("carol")).thenReturn(List.of(n1, n2));
        when(notificationRepository.findByUsernameAndReadFalseOrderByCreatedAtDesc("carol")).thenReturn(List.of(n1));

        List<Notification> all = notificationService.getNotifications("carol");
        assertEquals(2, all.size());

        List<Notification> unread = notificationService.getUnreadNotifications("carol");
        assertEquals(1, unread.size());
    }

    @Test
    @DisplayName("Debe marcar como leída una notificación propia del usuario")
    void testMarkAsReadSuccess() {
        Notification n = new Notification("carol", 10L, "Mensaje 1");
        n.setRead(false);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification read = notificationService.markAsRead(1L, "carol");

        assertTrue(read.isRead());
        verify(notificationRepository).save(n);
    }

    @Test
    @DisplayName("Debe lanzar AccessDeniedException si se intenta marcar como leída una notificación de otro usuario")
    void testMarkAsReadDifferentUserFails() {
        Notification n = new Notification("dave", 10L, "Mensaje de Dave");
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));

        assertThrows(AccessDeniedException.class, () -> notificationService.markAsRead(1L, "carol"));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si la notificación no existe")
    void testMarkAsReadNotFoundFails() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.markAsRead(99L, "carol"));
    }
}
