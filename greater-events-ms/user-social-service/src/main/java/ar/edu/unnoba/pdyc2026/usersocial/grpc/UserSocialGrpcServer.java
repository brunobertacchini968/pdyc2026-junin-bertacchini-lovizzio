package ar.edu.unnoba.pdyc2026.usersocial.grpc;

import ar.edu.unnoba.pdyc2026.grpc.usersocial.*;
import ar.edu.unnoba.pdyc2026.usersocial.model.User;
import ar.edu.unnoba.pdyc2026.usersocial.repository.UserRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.stream.Collectors;

@GrpcService
public class UserSocialGrpcServer extends UserSocialGrpcServiceGrpc.UserSocialGrpcServiceImplBase {

    private final UserRepository userRepository;

    public UserSocialGrpcServer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void getFollowersByArtistId(ArtistFollowersRequest request, StreamObserver<UserIdsResponse> responseObserver) {
        Long artistId = request.getArtistId();
        List<User> followers = userRepository.findDistinctByFollowedArtistId(artistId);
        List<String> usernames = followers.stream().map(User::getUsername).collect(Collectors.toList());

        UserIdsResponse response = UserIdsResponse.newBuilder()
                .addAllUsernames(usernames)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getFavoritersByEventId(EventFavoritersRequest request, StreamObserver<UserIdsResponse> responseObserver) {
        Long eventId = request.getEventId();
        List<User> favoriters = userRepository.findDistinctByFavoriteEventId(eventId);
        List<String> usernames = favoriters.stream().map(User::getUsername).collect(Collectors.toList());

        UserIdsResponse response = UserIdsResponse.newBuilder()
                .addAllUsernames(usernames)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
