package ar.edu.unnoba.pdyc2026.usersocial.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_followed_artists",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "artist_id")
    private Set<Long> followedArtists = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_favorite_events",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "event_id")
    private Set<Long> favoriteEvents = new HashSet<>();

    public User() {
    }

    public User(String username, String email) {
        this.username = username;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<Long> getFollowedArtists() {
        return followedArtists;
    }

    public void setFollowedArtists(Set<Long> followedArtists) {
        this.followedArtists = followedArtists;
    }

    public Set<Long> getFavoriteEvents() {
        return favoriteEvents;
    }

    public void setFavoriteEvents(Set<Long> favoriteEvents) {
        this.favoriteEvents = favoriteEvents;
    }
}
