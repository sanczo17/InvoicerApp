package org.example.repository;

import org.example.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repozytorium dla encji User.
 * Dostarcza metody do operacji na bazie danych dla użytkowników.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Znajduje użytkownika po nazwie użytkownika.
     *
     * @param username nazwa użytkownika do znalezienia
     * @return opcjonalny użytkownik (może nie istnieć w bazie)
     */
    Optional<User> findByUsername(String username);

    /**
     * Sprawdza czy istnieje użytkownik o podanej nazwie użytkownika.
     *
     * @param username nazwa użytkownika do sprawdzenia
     * @return true jeśli użytkownik istnieje, false w przeciwnym przypadku
     */
    Boolean existsByUsername(String username);

    /**
     * Sprawdza czy istnieje użytkownik o podanym adresie email.
     *
     * @param email email do sprawdzenia
     * @return true jeśli użytkownik istnieje, false w przeciwnym przypadku
     */
    Boolean existsByEmail(String email);
}