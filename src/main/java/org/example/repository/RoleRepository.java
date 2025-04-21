package org.example.repository;

import org.example.model.Role;
import org.example.model.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repozytorium dla encji Role.
 * Dostarcza metody do operacji na bazie danych dla ról użytkowników.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Znajduje rolę po nazwie (typie).
     *
     * @param name typ roli do znalezienia
     * @return opcjonalna rola (może nie istnieć w bazie)
     */
    Optional<Role> findByName(RoleType name);
}