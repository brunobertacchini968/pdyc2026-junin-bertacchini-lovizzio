package ar.edu.unnoba.pdyc2026.events.controller;

import ar.edu.unnoba.pdyc2026.events.exception.BusinessException;
import ar.edu.unnoba.pdyc2026.events.exception.GlobalExceptionHandler;
import ar.edu.unnoba.pdyc2026.events.exception.ResourceNotFoundException;
import ar.edu.unnoba.pdyc2026.events.model.Artist;
import ar.edu.unnoba.pdyc2026.events.model.Genre;
import ar.edu.unnoba.pdyc2026.events.service.ArtistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ArtistController.class)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = "spring.jackson.property-naming-strategy=SNAKE_CASE")
class ArtistControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArtistService artistService;

    @Test
    void getArtists_returnsSummaries() throws Exception {
        Artist artist = artist(1L, "Soda Stereo", Genre.ROCK, false);
        given(artistService.findAll(null)).willReturn(List.of(artist));

        mockMvc.perform(get("/admin/artists").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Soda Stereo"))
                .andExpect(jsonPath("$[0].genre").value("ROCK"))
                .andExpect(jsonPath("$[0].active").value(false));
    }

    @Test
    void getArtists_filtersByGenre() throws Exception {
        Artist artist = artist(2L, "Patricia", Genre.ROCK, true);
        given(artistService.findAll(Genre.ROCK)).willReturn(List.of(artist));

        mockMvc.perform(get("/admin/artists")
                        .param("genre", "rock")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2L))
                .andExpect(jsonPath("$[0].name").value("Patricia"))
                .andExpect(jsonPath("$[0].genre").value("ROCK"));

        verify(artistService).findAll(Genre.ROCK);
    }

    @Test
    void getArtist_returnsDetail() throws Exception {
        Artist artist = artist(5L, "Babasónicos", Genre.POP, true);
        given(artistService.findById(5L)).willReturn(artist);

        mockMvc.perform(get("/admin/artists/5").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.name").value("Babasónicos"))
                .andExpect(jsonPath("$.genre").value("POP"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void getArtist_notFound_returns404() throws Exception {
        given(artistService.findById(9L)).willThrow(new ResourceNotFoundException("Artist not found with id: 9"));

        mockMvc.perform(get("/admin/artists/9").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Artist not found with id: 9"));
    }

    @Test
    void getArtist_invalidIdType_returns400() throws Exception {
        mockMvc.perform(get("/admin/artists/abc").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid value for parameter 'id'"));
    }

    @Test
    void createArtist_valid_returns201() throws Exception {
        Artist created = artist(10L, "Airbag", Genre.ROCK, true);
        given(artistService.create("Airbag", Genre.ROCK)).willReturn(created);

        mockMvc.perform(post("/admin/artists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Airbag","genre":"rock"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.name").value("Airbag"))
                .andExpect(jsonPath("$.genre").value("ROCK"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createArtist_invalidGenre_returns400() throws Exception {
        mockMvc.perform(post("/admin/artists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Airbag","genre":"metal"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Invalid genre")));
    }

    @Test
    void createArtist_malformedJson_returns400() throws Exception {
        String malformedJson = java.util.UUID.randomUUID().toString();

        mockMvc.perform(post("/admin/artists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void updateArtist_valid_returns200() throws Exception {
        Artist updated = artist(3L, "Nuevos", Genre.JAZZ, true);
        given(artistService.update(3L, "Nuevos", Genre.JAZZ)).willReturn(updated);

        mockMvc.perform(put("/admin/artists/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Nuevos","genre":"jazz"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3L))
                .andExpect(jsonPath("$.name").value("Nuevos"))
                .andExpect(jsonPath("$.genre").value("JAZZ"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void updateArtist_blankName_returns400() throws Exception {
        Artist mockArtist = artist(3L, "Original", Genre.JAZZ, true);
        given(artistService.update(3L, "", Genre.JAZZ))
                .willThrow(new BusinessException("Artist name cannot be blank"));

        mockMvc.perform(put("/admin/artists/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","genre":"jazz"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("blank")));
    }

    @Test
    void deleteArtist_returnsNoContent() throws Exception {
        willDoNothing().given(artistService).delete(999L);

        mockMvc.perform(delete("/admin/artists/999"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    private Artist artist(Long id, String name, Genre genre, boolean active) {
        Artist artist = new Artist(name, genre);
        artist.setId(id);
        artist.setActive(active);
        return artist;
    }
}








