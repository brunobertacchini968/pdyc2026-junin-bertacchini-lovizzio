package ar.edu.unnoba.pdyc2026.events.repository;

import ar.edu.unnoba.pdyc2026.events.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    java.util.List<User> findDistinctByFollowedArtistsId(Long artistId);

    java.util.List<User> findDistinctByFavoriteEventsId(Long eventId);
}
