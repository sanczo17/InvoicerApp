package org.example.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

/**
 * Encja reprezentująca użytkownika systemu.
 * Przechowuje dane uwierzytelniające oraz informacje o rolach i statusie użytkownika.
 */
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

    /**
     * Flaga określająca czy konto jest aktywne.
     * Nieaktywne konta nie mogą się logować do systemu.
     */
    private boolean active = true;

    /**
     * Flaga określająca czy użytkownik musi zmienić hasło przy następnym logowaniu.
     * Używana dla nowych kont lub po resecie hasła.
     */
    private boolean mustChangePassword = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    /**
     * Konstruktor domyślny wymagany przez JPA.
     */
    public User() {
    }

    /**
     * Zwraca identyfikator użytkownika.
     */
    public Long getId() {
        return id;
    }

    /**
     * Ustawia identyfikator użytkownika.
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Zwraca nazwę użytkownika.
     */
    public @NotBlank(message = "Nazwa użytkownika jest wymagana") @Size(min = 3, max = 50, message = "Nazwa użytkownika musi mieć od 3 do 50 znaków") String getUsername() {
        return username;
    }

    /**
     * Ustawia nazwę użytkownika.
     */
    public void setUsername(@NotBlank(message = "Nazwa użytkownika jest wymagana") @Size(min = 3, max = 50, message = "Nazwa użytkownika musi mieć od 3 do 50 znaków") String username) {
        this.username = username;
    }

    /**
     * Zwraca zakodowane hasło użytkownika.
     */
    public @NotBlank(message = "Hasło jest wymagane") String getPassword() {
        return password;
    }

    /**
     * Ustawia hasło użytkownika.
     * Hasło powinno być zakodowane przed zapisem do bazy.
     */
    public void setPassword(@NotBlank(message = "Hasło jest wymagane") String password) {
        this.password = password;
    }

    /**
     * Zwraca email użytkownika.
     */
    public @Email(message = "Niepoprawny format adresu email") String getEmail() {
        return email;
    }

    /**
     * Ustawia email użytkownika.
     */
    public void setEmail(@Email(message = "Niepoprawny format adresu email") String email) {
        this.email = email;
    }

    /**
     * Sprawdza czy konto użytkownika jest aktywne.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Ustawia status aktywności konta użytkownika.
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Sprawdza czy użytkownik musi zmienić hasło przy następnym logowaniu.
     */
    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    /**
     * Ustawia flagę wymagania zmiany hasła.
     */
    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    /**
     * Zwraca zbiór ról przypisanych użytkownikowi.
     */
    public Set<Role> getRoles() {
        return roles;
    }

    /**
     * Ustawia zbiór ról użytkownika.
     */
    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
}