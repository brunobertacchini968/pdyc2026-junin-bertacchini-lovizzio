package ar.edu.unnoba.pdyc2026.events.service;

import ar.edu.unnoba.pdyc2026.events.dto.AdminUserDTO;
import ar.edu.unnoba.pdyc2026.events.dto.CreateAdminUserDTO;
import ar.edu.unnoba.pdyc2026.events.exception.BusinessException;
import ar.edu.unnoba.pdyc2026.events.exception.ResourceNotFoundException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final Keycloak keycloak;
    private final String realm;
    private final Executor executor;

    public AdminUserServiceImpl(
            Keycloak keycloak,
            @Value("${keycloak.realm}") String realm,
            @Qualifier("keycloakExecutor") Executor executor) {
        this.keycloak = keycloak;
        this.realm = realm;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<List<AdminUserDTO>> findAll() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<UserRepresentation> users = keycloak.realm(realm).users().list();
                return users.stream()
                        .map(AdminUserDTO::fromUserRepresentation)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                throw new BusinessException("Failed to retrieve admin users: " + e.getMessage());
            }
        }, executor);
    }

    @Override
    public CompletableFuture<AdminUserDTO> findById(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UserRepresentation user = keycloak.realm(realm).users().get(id).toRepresentation();
                return AdminUserDTO.fromUserRepresentation(user);
            } catch (NotFoundException e) {
                throw new ResourceNotFoundException("Admin user not found with id: " + id);
            } catch (WebApplicationException e) {
                if (e.getResponse().getStatus() == 404) {
                    throw new ResourceNotFoundException("Admin user not found with id: " + id);
                }
                throw new BusinessException("Keycloak API error: " + e.getMessage());
            } catch (Exception e) {
                throw new BusinessException("Failed to retrieve admin user: " + e.getMessage());
            }
        }, executor);
    }

    @Override
    public CompletableFuture<AdminUserDTO> create(CreateAdminUserDTO dto) {
        return CompletableFuture.supplyAsync(() -> {
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(dto.getPassword());
            credential.setTemporary(false);

            UserRepresentation user = new UserRepresentation();
            user.setUsername(dto.getUsername());
            user.setEmail(dto.getEmail());
            user.setFirstName(dto.getFirstName());
            user.setLastName(dto.getLastName());
            user.setEnabled(true);
            user.setCredentials(List.of(credential));

            try (Response response = keycloak.realm(realm).users().create(user)) {
                if (response.getStatus() == 409) {
                    throw new BusinessException("Username or email already exists");
                } else if (response.getStatus() != 201) {
                    throw new BusinessException("Failed to create user in Keycloak. Status: " + response.getStatus());
                }

                String createdId = CreatedResponseUtil.getCreatedId(response);
                user.setId(createdId);
                return AdminUserDTO.fromUserRepresentation(user);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                throw new BusinessException("Failed to create admin user: " + e.getMessage());
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> delete(String id) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Check if user exists first to throw correct exception
                try {
                    keycloak.realm(realm).users().get(id).toRepresentation();
                } catch (NotFoundException e) {
                    throw new ResourceNotFoundException("Admin user not found with id: " + id);
                } catch (WebApplicationException e) {
                    if (e.getResponse().getStatus() == 404) {
                        throw new ResourceNotFoundException("Admin user not found with id: " + id);
                    }
                    throw e;
                }

                try (Response response = keycloak.realm(realm).users().delete(id)) {
                    if (response.getStatus() != 204 && response.getStatus() != 200) {
                        throw new BusinessException("Failed to delete user from Keycloak. Status: " + response.getStatus());
                    }
                }
            } catch (ResourceNotFoundException e) {
                throw e;
            } catch (Exception e) {
                throw new BusinessException("Failed to delete admin user: " + e.getMessage());
            }
        }, executor);
    }
}
