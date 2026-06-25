package ar.edu.unnoba.pdyc2026.usersocial.service;

import ar.edu.unnoba.pdyc2026.usersocial.dto.CreateUserDTO;
import ar.edu.unnoba.pdyc2026.usersocial.dto.UserDTO;

import java.util.concurrent.CompletableFuture;

public interface AuthService {
    CompletableFuture<UserDTO> register(CreateUserDTO dto);
}
