package ar.edu.unnoba.pdyc2026.events.service;

import ar.edu.unnoba.pdyc2026.events.dto.CreateUserDTO;
import ar.edu.unnoba.pdyc2026.events.dto.UserDTO;

import java.util.concurrent.CompletableFuture;

public interface AuthService {

    CompletableFuture<UserDTO> register(CreateUserDTO dto);
}
