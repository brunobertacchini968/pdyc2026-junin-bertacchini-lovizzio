package ar.edu.unnoba.pdyc2026.events.dto;

import org.keycloak.representations.idm.UserRepresentation;

public class AdminUserDTO {

    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private boolean enabled;

    public AdminUserDTO() {
    }

    public AdminUserDTO(String id, String username, String email, String firstName, String lastName, boolean enabled) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.enabled = enabled;
    }

    public static AdminUserDTO fromUserRepresentation(UserRepresentation user) {
        if (user == null) {
            return null;
        }
        return new AdminUserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.isEnabled() != null ? user.isEnabled() : false
        );
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
