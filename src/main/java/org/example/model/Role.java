package org.example.model;

import jakarta.persistence.*;
import org.example.model.enums.RoleType;

/**
 * Encja reprezentująca rolę użytkownika w systemie.
 * Używana do kontroli dostępu i uprawnień użytkowników.
 */
@Entity
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true)
    private RoleType name;

    /**
     * Konstruktor domyślny wymagany przez JPA.
     */
    public Role() {
    }

    /**
     * Tworzy rolę z określonym typem.
     *
     * @param name typ roli (ROLE_ADMIN lub ROLE_USER)
     */
    public Role(RoleType name) {
        this.name = name;
    }

    /**
     * Zwraca identyfikator roli.
     */
    public Long getId() {
        return id;
    }

    /**
     * Ustawia identyfikator roli.
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Zwraca typ roli.
     */
    public RoleType getName() {
        return name;
    }

    /**
     * Ustawia typ roli.
     */
    public void setName(RoleType name) {
        this.name = name;
    }
}