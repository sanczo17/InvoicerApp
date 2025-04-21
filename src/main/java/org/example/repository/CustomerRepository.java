package org.example.repository;

import org.example.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // Metoda do wyszukiwania klientów po nazwie (zawierającej podaną frazę)
    List<Customer> findByNameContaining(String name);

    // Metoda do wyszukiwania klientów po nipie (zawierającym podaną frazę)
    List<Customer> findByNipContaining(String nip);

    // Metoda do wyszukiwania klientów po emailu
    List<Customer> findByEmail(String email);

    // Zaawansowane wyszukiwanie klientów
    @Query("SELECT c FROM Customer c WHERE " +
            "(:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:nip IS NULL OR c.nip LIKE CONCAT('%', :nip, '%')) AND " +
            "(:email IS NULL OR c.email = :email)")
    List<Customer> searchCustomers(
            @Param("name") String name,
            @Param("nip") String nip,
            @Param("email") String email);
}