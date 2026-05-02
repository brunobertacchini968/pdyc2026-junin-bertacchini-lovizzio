package ar.edu.unnoba.pdyc2026.events.controller;

import ar.edu.unnoba.pdyc2026.events.exception.GlobalExceptionHandler;
import ar.edu.unnoba.pdyc2026.events.service.ArtistService;
import ar.edu.unnoba.pdyc2026.events.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ArtistController.class, EventController.class})
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = "spring.jackson.property-naming-strategy=SNAKE_CASE")
class ValidationWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArtistService artistService;

    @MockitoBean
    private EventService eventService;

    // ===== GENRE VALIDATION TESTS =====

    @Test
    void createArtist_invalidGenre_returnsClearError() throws Exception {
        mockMvc.perform(post("/admin/artists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Test Band","genre":"Heavy Metal"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Invalid genre")))
                .andExpect(jsonPath("$.error", containsString("Heavy Metal")))
                .andExpect(jsonPath("$.error", containsString("Valid values")));
    }

    @Test
    void createArtist_missingGenre_validationError() throws Exception {
        mockMvc.perform(post("/admin/artists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Test Band"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("genre")))
                .andExpect(jsonPath("$.error", containsString("required")));
    }

    @Test
    void createArtist_blankName_validationError() throws Exception {
        mockMvc.perform(post("/admin/artists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","genre":"rock"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("name")))
                .andExpect(jsonPath("$.error", containsString("blank")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createArtist_nullGenreValue_validationError() throws Exception {
        mockMvc.perform(post("/admin/artists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Test Band","genre":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("genre")))
                .andExpect(jsonPath("$.error", containsString("required")));
    }

    // ===== EVENT VALIDATION TESTS =====

    @Test
    void createEvent_bothFieldsMissing_returnsError() throws Exception {
        mockMvc.perform(post("/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Sample"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("start_date")))
                .andExpect(jsonPath("$.error", containsString("required")));
    }

    @Test
    void createEvent_anyFieldMissing_returnsValidationError() throws Exception {
        mockMvc.perform(post("/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Sample"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error", containsString("required")));
    }

    @Test
    void createEvent_missingName_validationError() throws Exception {
        mockMvc.perform(post("/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"start_date":"2026-12-25"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("name")))
                .andExpect(jsonPath("$.error", containsString("required")));
    }

    @Test
    void createEvent_blankNameRejectsWrite_validationError() throws Exception {
        mockMvc.perform(post("/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","start_date":"2026-12-25"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("name")))
                .andExpect(jsonPath("$.error", containsString("blank")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEvent_missingStartDate_validationError() throws Exception {
        mockMvc.perform(post("/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Test Event"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("start_date")))
                .andExpect(jsonPath("$.error", containsString("required")));
    }

    @Test
    void createEvent_nullStartDate_validationError() throws Exception {
        mockMvc.perform(post("/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Test Event","start_date":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("start_date")))
                .andExpect(jsonPath("$.error", containsString("required")));
    }

    @Test
    void createEvent_invalidDateFormat_parseDateError() throws Exception {
        mockMvc.perform(post("/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Test Event","start_date":"not-a-date"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(status().isBadRequest());
    }

    // ===== TYPE VALIDATION TESTS =====

    @Test
    void createArtist_genreAsNumber_parseError() throws Exception {
        mockMvc.perform(post("/admin/artists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Test Band","genre":123}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEvent_invalidDateString_parseError() throws Exception {
        mockMvc.perform(post("/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"My Event","description":"Test","start_date":"not-a-date"}
                                """))
                .andExpect(status().isBadRequest());
    }

}


















