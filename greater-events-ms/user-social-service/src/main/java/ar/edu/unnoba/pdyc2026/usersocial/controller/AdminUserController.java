package ar.edu.unnoba.pdyc2026.usersocial.controller;

import ar.edu.unnoba.pdyc2026.usersocial.dto.AdminUserDTO;
import ar.edu.unnoba.pdyc2026.usersocial.dto.CreateAdminUserDTO;
import ar.edu.unnoba.pdyc2026.usersocial.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public CompletableFuture<List<AdminUserDTO>> getAdminUsers() {
        return adminUserService.findAll();
    }

    @GetMapping("/{id}")
    public CompletableFuture<AdminUserDTO> getAdminUser(@PathVariable String id) {
        return adminUserService.findById(id);
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<AdminUserDTO>> createAdminUser(@Valid @RequestBody CreateAdminUserDTO body) {
        return adminUserService.create(body)
                .thenApply(user -> ResponseEntity.status(HttpStatus.CREATED).body(user));
    }

    @DeleteMapping("/{id}")
    public CompletableFuture<ResponseEntity<Void>> deleteAdminUser(@PathVariable String id) {
        return adminUserService.delete(id)
                .thenApply(v -> ResponseEntity.noContent().build());
    }
}
