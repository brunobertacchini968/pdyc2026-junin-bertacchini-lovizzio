package ar.edu.unnoba.pdyc2026.usersocial.service;

import ar.edu.unnoba.pdyc2026.usersocial.dto.ArtistDTO;
import ar.edu.unnoba.pdyc2026.usersocial.dto.EventSummaryDTO;

import java.util.List;

public interface UserService {

    void followArtist(String username, Long artistId);

    void unfollowArtist(String username, Long artistId);

    List<ArtistDTO> getFollowedArtists(String username);

    void favoriteEvent(String username, Long eventId);

    void unfavoriteEvent(String username, Long eventId);

    List<EventSummaryDTO> getFavoriteEvents(String username);

    List<EventSummaryDTO> getFollowingEvents(String username);
}
