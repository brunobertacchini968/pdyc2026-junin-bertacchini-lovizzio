package ar.edu.unnoba.pdyc2026.usersocial.feign;

import ar.edu.unnoba.pdyc2026.usersocial.dto.ArtistDTO;
import ar.edu.unnoba.pdyc2026.usersocial.dto.EventSummaryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "catalog-service")
public interface CatalogFeignClient {

    @GetMapping("/artists/{id}")
    ArtistDTO getArtistById(@PathVariable("id") Long id);

    @PostMapping("/artists/search/by-ids")
    List<ArtistDTO> getArtistsByIds(@RequestBody List<Long> artistIds);

    @GetMapping("/events/{id}")
    Object getEventById(@PathVariable("id") Long id);

    @PostMapping("/events/search/by-ids")
    List<EventSummaryDTO> getEventsByIds(@RequestBody List<Long> eventIds);

    @PostMapping("/events/upcoming/by-artists")
    List<EventSummaryDTO> getUpcomingEventsForArtists(@RequestBody List<Long> artistIds);
}
