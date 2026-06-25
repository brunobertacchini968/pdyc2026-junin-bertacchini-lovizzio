package ar.edu.unnoba.pdyc2026.usersocial.repository;

import ar.edu.unnoba.pdyc2026.usersocial.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("select distinct u from User u join u.followedArtists a where a = :artistId")
    List<User> findDistinctByFollowedArtistId(@Param("artistId") Long artistId);

    @Query("select distinct u from User u join u.favoriteEvents e where e = :eventId")
    List<User> findDistinctByFavoriteEventId(@Param("eventId") Long eventId);
}
