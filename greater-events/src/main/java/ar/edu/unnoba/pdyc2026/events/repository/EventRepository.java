package ar.edu.unnoba.pdyc2026.events.repository;

import ar.edu.unnoba.pdyc2026.events.model.Event;
import ar.edu.unnoba.pdyc2026.events.model.EventState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDate;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByState(EventState state);

    List<Event> findDistinctByArtistsIdAndStateInAndStartDateAfterOrderByStartDateAsc(Long artistId, List<EventState> states, LocalDate startDate);
}
