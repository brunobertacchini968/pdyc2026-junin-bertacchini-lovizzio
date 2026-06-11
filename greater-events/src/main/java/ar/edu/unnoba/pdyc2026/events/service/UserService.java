package ar.edu.unnoba.pdyc2026.events.service;

import ar.edu.unnoba.pdyc2026.events.model.Artist;
import ar.edu.unnoba.pdyc2026.events.model.Event;

import java.util.List;

public interface UserService {

    void followArtist(String username, Long artistId);

    void unfollowArtist(String username, Long artistId);

    List<Artist> getFollowedArtists(String username);

    void favoriteEvent(String username, Long eventId);

    void unfavoriteEvent(String username, Long eventId);

    List<Event> getFavoriteEvents(String username);

    List<Event> getFollowingEvents(String username);
}
