package ar.edu.unnoba.pdyc2026.notification.messaging;

import ar.edu.unnoba.pdyc2026.notification.grpc.UserSocialGrpcClient;
import ar.edu.unnoba.pdyc2026.notification.model.Notification;
import ar.edu.unnoba.pdyc2026.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventCancelledListenerTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserSocialGrpcClient userSocialGrpcClient;

    @InjectMocks
    private EventCancelledListener listener;

    @Test
    @DisplayName("Debe desduplicar destinatarios y guardar una notificación por usuario")
    void testHandleEventCancelledDeduplication() {
        EventCancelledMessage msg = new EventCancelledMessage(10L, "Festival Quilmes", List.of(1L, 2L));

        // user1 y user2 son seguidores
        when(userSocialGrpcClient.getFollowersByArtistId(1L)).thenReturn(List.of("user1", "user2"));
        when(userSocialGrpcClient.getFollowersByArtistId(2L)).thenReturn(List.of("user2", "user3"));
        // user1 y user4 tenían el evento en favoritos
        when(userSocialGrpcClient.getFavoritersByEventId(10L)).thenReturn(List.of("user1", "user4"));

        listener.handleEventCancelled(msg);

        // Debe haber exactamente 4 destinatarios únicos: user1, user2, user3, user4
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(4)).save(captor.capture());

        List<Notification> saved = captor.getAllValues();
        List<String> usernames = saved.stream().map(Notification::getUsername).toList();
        assertTrue(usernames.containsAll(List.of("user1", "user2", "user3", "user4")));
        assertEquals(4, usernames.stream().distinct().count());
    }

    @Test
    @DisplayName("Debe procesar mensaje sin artistas asociados sin lanzar NPE")
    void testHandleEventCancelledNullArtists() {
        EventCancelledMessage msg = new EventCancelledMessage(20L, "Acústico Solitario", null);
        when(userSocialGrpcClient.getFavoritersByEventId(20L)).thenReturn(List.of("fan1"));

        assertDoesNotThrow(() -> listener.handleEventCancelled(msg));

        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(userSocialGrpcClient, never()).getFollowersByArtistId(any());
    }
}
