package ar.edu.unnoba.pdyc2026.events.controller;

import ar.edu.unnoba.pdyc2026.events.dto.AdminUserDTO;
import ar.edu.unnoba.pdyc2026.events.dto.CreateAdminUserDTO;
import ar.edu.unnoba.pdyc2026.events.exception.GlobalExceptionHandler;
import ar.edu.unnoba.pdyc2026.events.exception.ResourceNotFoundException;
import ar.edu.unnoba.pdyc2026.events.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminUserController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "spring.jackson.property-naming-strategy=SNAKE_CASE")
class AdminUserControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService adminUserService;

    @Test
    void getAdminUsers_returnsList() throws Exception {
        AdminUserDTO user = new AdminUserDTO("uuid-123", "admin_bob", "bob@example.com", "Bob", "Builder", true);
        given(adminUserService.findAll()).willReturn(CompletableFuture.completedFuture(List.of(user)));

        MvcResult mvcResult = mockMvc.perform(get("/admin/users").accept(MediaType.APPLICATION_JSON))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value("uuid-123"))
                .andExpect(jsonPath("$[0].username").value("admin_bob"))
                .andExpect(jsonPath("$[0].email").value("bob@example.com"))
                .andExpect(jsonPath("$[0].first_name").value("Bob"))
                .andExpect(jsonPath("$[0].last_name").value("Builder"))
                .andExpect(jsonPath("$[0].enabled").value(true));
    }

    @Test
    void getAdminUser_returnsUser() throws Exception {
        AdminUserDTO user = new AdminUserDTO("uuid-456", "admin_alice", "alice@example.com", "Alice", "Wonderland", true);
        given(adminUserService.findById("uuid-456")).willReturn(CompletableFuture.completedFuture(user));

        MvcResult mvcResult = mockMvc.perform(get("/admin/users/uuid-456").accept(MediaType.APPLICATION_JSON))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("uuid-456"))
                .andExpect(jsonPath("$.username").value("admin_alice"))
                .andExpect(jsonPath("$.first_name").value("Alice"));
    }

    @Test
    void getAdminUser_notFound_returns404() throws Exception {
        CompletableFuture<AdminUserDTO> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new ResourceNotFoundException("Admin user not found with id: unknown"));
        given(adminUserService.findById("unknown")).willReturn(failedFuture);

        MvcResult mvcResult = mockMvc.perform(get("/admin/users/unknown").accept(MediaType.APPLICATION_JSON))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Admin user not found with id: unknown"));
    }

    @Test
    void createAdminUser_valid_returns201() throws Exception {
        AdminUserDTO created = new AdminUserDTO("uuid-new", "charlie", "charlie@example.com", "Charlie", "Brown", true);
        given(adminUserService.create(any(CreateAdminUserDTO.class))).willReturn(CompletableFuture.completedFuture(created));

        MvcResult mvcResult = mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "charlie",
                                  "email": "charlie@example.com",
                                  "password": "secure123Password",
                                  "first_name": "Charlie",
                                  "last_name": "Brown"
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("uuid-new"))
                .andExpect(jsonPath("$.username").value("charlie"))
                .andExpect(jsonPath("$.email").value("charlie@example.com"));

        ArgumentCaptor<CreateAdminUserDTO> captor = ArgumentCaptor.forClass(CreateAdminUserDTO.class);
        verify(adminUserService).create(captor.capture());
        CreateAdminUserDTO captured = captor.getValue();
        assertEquals("charlie", captured.getUsername());
        assertEquals("charlie@example.com", captured.getEmail());
        assertEquals("secure123Password", captured.getPassword());
    }

    @Test
    void createAdminUser_invalidPayload_returns400() throws Exception {
        // Validation fails BEFORE service is called, so it won't be asyncStarted
        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "",
                                  "email": "invalid-email",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("cannot be blank")));
    }

    @Test
    void deleteAdminUser_returns204() throws Exception {
        given(adminUserService.delete("uuid-delete")).willReturn(CompletableFuture.completedFuture(null));

        MvcResult mvcResult = mockMvc.perform(delete("/admin/users/uuid-delete"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }
}
