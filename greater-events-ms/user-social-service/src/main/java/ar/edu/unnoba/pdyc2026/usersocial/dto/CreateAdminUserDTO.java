package ar.edu.unnoba.pdyc2026.usersocial.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class CreateAdminUserDTO {

    @NotBlank(message = "Username is required and cannot be blank")
    private String username;

    @NotBlank(message = "Email is required and cannot be blank")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required and cannot be blank")
    private String password;

    private String firstName;
    private String lastName;

    public CreateAdminUserDTO() {
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
}
