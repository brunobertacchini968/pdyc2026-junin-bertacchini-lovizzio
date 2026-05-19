package ar.edu.unnoba.pdyc2026.events.service;

import ar.edu.unnoba.pdyc2026.events.dto.AdminUserDTO;
import ar.edu.unnoba.pdyc2026.events.dto.CreateAdminUserDTO;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AdminUserService {

    CompletableFuture<List<AdminUserDTO>> findAll();

    CompletableFuture<AdminUserDTO> findById(String id);

    CompletableFuture<AdminUserDTO> create(CreateAdminUserDTO dto);

    CompletableFuture<Void> delete(String id);
}
