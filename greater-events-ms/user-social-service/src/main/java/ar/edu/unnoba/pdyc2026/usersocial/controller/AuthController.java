package ar.edu.unnoba.pdyc2026.usersocial.controller;

import ar.edu.unnoba.pdyc2026.usersocial.dto.CreateUserDTO;
import ar.edu.unnoba.pdyc2026.usersocial.dto.UserDTO;
import ar.edu.unnoba.pdyc2026.usersocial.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public CompletableFuture<ResponseEntity<UserDTO>> register(@Valid @RequestBody CreateUserDTO body) {
        return authService.register(body)
                .thenApply(user -> ResponseEntity.status(HttpStatus.CREATED).body(user));
    }
}
