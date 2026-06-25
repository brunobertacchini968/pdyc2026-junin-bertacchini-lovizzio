package ar.edu.unnoba.pdyc2026.catalog.repository;

import ar.edu.unnoba.pdyc2026.catalog.model.Event;
import ar.edu.unnoba.pdyc2026.catalog.model.EventState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByState(EventState state);

    List<Event> findDistinctByArtistsIdAndStateInAndStartDateAfterOrderByStartDateAsc(
            Long artistId, List<EventState> states, LocalDate startDate);

    List<Event> findDistinctByArtistsIdInAndStateInAndStartDateAfterOrderByStartDateAsc(
            List<Long> artistIds, List<EventState> states, LocalDate startDate);
}
