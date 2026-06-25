package ar.edu.unnoba.pdyc2026.usersocial.service;

import ar.edu.unnoba.pdyc2026.usersocial.dto.CreateUserDTO;
import ar.edu.unnoba.pdyc2026.usersocial.dto.UserDTO;
import ar.edu.unnoba.pdyc2026.usersocial.exception.BusinessException;
import ar.edu.unnoba.pdyc2026.usersocial.model.User;
import ar.edu.unnoba.pdyc2026.usersocial.repository.UserRepository;
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

@Service
public class AuthServiceImpl implements AuthService {

    private final Keycloak keycloak;
    private final String realm;
    private final Executor executor;
    private final UserRepository userRepository;

    public AuthServiceImpl(Keycloak keycloak,
                           @Value("${keycloak.realm}") String realm,
                           @Qualifier("keycloakExecutor") Executor executor,
                           UserRepository userRepository) {
        this.keycloak = keycloak;
        this.realm = realm;
        this.executor = executor;
        this.userRepository = userRepository;
    }

    @Override
    public CompletableFuture<UserDTO> register(CreateUserDTO dto) {
        return CompletableFuture.supplyAsync(() -> {
            if (userRepository.existsByUsername(dto.getUsername())) {
                throw new BusinessException("Username already exists");
            }
            if (userRepository.existsByEmail(dto.getEmail())) {
                throw new BusinessException("Email already exists");
            }

            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(dto.getPassword());
            credential.setTemporary(false);

            UserRepresentation userRep = new UserRepresentation();
            userRep.setUsername(dto.getUsername());
            userRep.setEmail(dto.getEmail());
            userRep.setFirstName(dto.getFirstName());
            userRep.setLastName(dto.getLastName());
            userRep.setEnabled(true);
            userRep.setCredentials(List.of(credential));

            try (Response response = keycloak.realm(realm).users().create(userRep)) {
                if (response.getStatus() == 409) {
                    throw new BusinessException("Username or email already exists in Keycloak");
                }
                if (response.getStatus() != 201) {
                    throw new BusinessException("Failed to create user in Keycloak. Status: " + response.getStatus());
                }

                String createdId = CreatedResponseUtil.getCreatedId(response);
                User saved = userRepository.save(new User(dto.getUsername(), dto.getEmail()));
                return UserDTO.fromEntity(saved);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                throw new BusinessException("Failed to register user: " + e.getMessage());
            }
        }, executor);
    }
}
