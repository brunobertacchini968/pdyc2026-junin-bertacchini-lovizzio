package ar.edu.unnoba.pdyc2026.notification.grpc;

import ar.edu.unnoba.pdyc2026.grpc.usersocial.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class UserSocialGrpcClient {

    @GrpcClient("user-social-service")
    private UserSocialGrpcServiceGrpc.UserSocialGrpcServiceBlockingStub userSocialStub;

    public List<String> getFollowersByArtistId(Long artistId) {
        UserIdsResponse response = userSocialStub.getFollowersByArtistId(
                ArtistFollowersRequest.newBuilder().setArtistId(artistId).build());
        return response.getUsernamesList();
    }

    public List<String> getFavoritersByEventId(Long eventId) {
        UserIdsResponse response = userSocialStub.getFavoritersByEventId(
                EventFavoritersRequest.newBuilder().setEventId(eventId).build());
        return response.getUsernamesList();
    }
}
