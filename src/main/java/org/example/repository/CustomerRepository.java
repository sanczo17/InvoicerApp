package org.example.repository;

import org.example.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repozytorium dla encji Customer.
 * Dostarcza metody do operacji na bazie danych dla klientów.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * Znajduje klientów, których nazwa zawiera podaną frazę.
     *
     * @param name fraza do wyszukania w nazwie klienta
     * @return lista klientów pasujących do kryterium
     */
    List<Customer> findByNameContaining(String name);

    /**
     * Znajduje klientów, których NIP zawiera podaną frazę.
     *
     * @param nip fraza do wyszukania w NIP-ie klienta
     * @return lista klientów pasujących do kryterium
     */
    List<Customer> findByNipContaining(String nip);

    /**
     * Znajduje klientów po dokładnym emailu.
     *
     * @param email email klienta do znalezienia
     * @return lista klientów z podanym emailem
     */
    List<Customer> findByEmail(String email);

    /**
     * Zaawansowane wyszukiwanie klientów z wieloma kryteriami.
     * Parametry mogą być null, co oznacza brak filtrowania po danym kryterium.
     *
     * @param name opcjonalna fraza do wyszukania w nazwie klienta
     * @param nip opcjonalna fraza do wyszukania w NIP-ie klienta
     * @param email opcjonalny dokładny email klienta
     * @return lista klientów pasujących do kryteriów
     */
    @Query("SELECT c FROM Customer c WHERE " +
            "(:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:nip IS NULL OR c.nip LIKE CONCAT('%', :nip, '%')) AND " +
            "(:email IS NULL OR c.email = :email)")
    List<Customer> searchCustomers(
            @Param("name") String name,
            @Param("nip") String nip,
            @Param("email") String email);
}