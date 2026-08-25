package ar.edu.unnoba.pdyc2026.notification;

import ar.edu.unnoba.pdyc2026.notification.grpc.UserSocialGrpcClient;
import ar.edu.unnoba.pdyc2026.notification.messaging.EventCancelledListener;
import ar.edu.unnoba.pdyc2026.notification.messaging.EventCancelledMessage;
import ar.edu.unnoba.pdyc2026.notification.model.Notification;
import ar.edu.unnoba.pdyc2026.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EventCancelledListener eventCancelledListener;

    @MockBean
    private UserSocialGrpcClient userSocialGrpcClient;

    @MockBean
    private org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory;

    @Test
    @DisplayName("Debe procesar el mensaje de evento cancelado y guardar notificaciones para seguidores y usuarios con evento favorito")
    void testProcessEventCancelledMessage() {
        Long eventId = 100L;
        Long artistId = 50L;
        EventCancelledMessage message = new EventCancelledMessage(eventId, "Festival de Rock", List.of(artistId));

        // Mock gRPC responses
        when(userSocialGrpcClient.getFollowersByArtistId(artistId)).thenReturn(List.of("user_follower1", "user_common"));
        when(userSocialGrpcClient.getFavoritersByEventId(eventId)).thenReturn(List.of("user_favoriter1", "user_common"));

        // Simular recepción del mensaje de RabbitMQ
        eventCancelledListener.handleEventCancelled(message);

        // Verificar que los usuarios recibieron la notificación
        List<Notification> followerNotifs = notificationService.getNotifications("user_follower1");
        assertEquals(1, followerNotifs.size());
        assertTrue(followerNotifs.get(0).getMessage().contains("Festival de Rock"));
        assertFalse(followerNotifs.get(0).isRead());

        List<Notification> commonNotifs = notificationService.getNotifications("user_common");
        assertEquals(1, commonNotifs.size(), "Debe desduplicar y guardar una sola notificación para el usuario común");

        // Marcar notificación como leída
        Notification readNotif = notificationService.markAsRead(followerNotifs.get(0).getId(), "user_follower1");
        assertTrue(readNotif.isRead());

        List<Notification> unreadNotifs = notificationService.getUnreadNotifications("user_follower1");
        assertTrue(unreadNotifs.isEmpty());
    }
}
