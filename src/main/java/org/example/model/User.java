package org.example.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nazwa użytkownika jest wymagana")
    @Size(min = 3, max = 50, message = "Nazwa użytkownika musi mieć od 3 do 50 znaków")
    @Column(unique = true)
    private String username;

    @NotBlank(message = "Hasło jest wymagane")
    private String password;

    @Email(message = "Niepoprawny format adresu email")
    @Column(unique = true)
    private String email;

    private boolean active = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<org.example.model.Role> roles = new HashSet<>();

    public User() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @NotBlank(message = "Nazwa użytkownika jest wymagana") @Size(min = 3, max = 50, message = "Nazwa użytkownika musi mieć od 3 do 50 znaków") String getUsername() {
        return username;
    }

    public void setUsername(@NotBlank(message = "Nazwa użytkownika jest wymagana") @Size(min = 3, max = 50, message = "Nazwa użytkownika musi mieć od 3 do 50 znaków") String username) {
        this.username = username;
    }

    public @NotBlank(message = "Hasło jest wymagane") String getPassword() {
        return password;
    }

    public void setPassword(@NotBlank(message = "Hasło jest wymagane") String password) {
        this.password = password;
    }

    public @Email(message = "Niepoprawny format adresu email") String getEmail() {
        return email;
    }

    public void setEmail(@Email(message = "Niepoprawny format adresu email") String email) {
        this.email = email;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
}