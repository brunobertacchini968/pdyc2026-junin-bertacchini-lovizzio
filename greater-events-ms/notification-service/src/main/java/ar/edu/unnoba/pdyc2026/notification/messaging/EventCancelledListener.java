package ar.edu.unnoba.pdyc2026.notification.messaging;

import ar.edu.unnoba.pdyc2026.notification.grpc.UserSocialGrpcClient;
import ar.edu.unnoba.pdyc2026.notification.model.Notification;
import ar.edu.unnoba.pdyc2026.notification.repository.NotificationRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class EventCancelledListener {

    private final NotificationRepository notificationRepository;
    private final UserSocialGrpcClient userSocialGrpcClient;

    public EventCancelledListener(NotificationRepository notificationRepository,
                                  UserSocialGrpcClient userSocialGrpcClient) {
        this.notificationRepository = notificationRepository;
        this.userSocialGrpcClient = userSocialGrpcClient;
    }

    @RabbitListener(queues = "${rabbitmq.queue}")
    public void handleEventCancelled(EventCancelledMessage message) {
        Set<String> recipients = new HashSet<>();

        // Get followers of all artists on the event
        if (message.getArtistIds() != null) {
            for (Long artistId : message.getArtistIds()) {
                List<String> followers = userSocialGrpcClient.getFollowersByArtistId(artistId);
                recipients.addAll(followers);
            }
        }

        // Get users who favorited the event
        List<String> favoriters = userSocialGrpcClient.getFavoritersByEventId(message.getEventId());
        recipients.addAll(favoriters);

        String notificationMessage = String.format(
                "El evento '%s' ha sido cancelado.", message.getEventName());

        recipients.forEach(username -> {
            Notification n = new Notification(username, message.getEventId(), notificationMessage);
            notificationRepository.save(n);
        });
    }
}
