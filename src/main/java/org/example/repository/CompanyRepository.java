package org.example.repository;

import org.example.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repozytorium dla encji Company.
 * Dostarcza metody do operacji na bazie danych dla informacji o firmie.
 */
@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    // Ponieważ w systemie powinien być tylko jeden rekord firmy,
    // możemy dodać metodę do łatwego pobierania pierwszego (i jedynego) rekordu
    default Company getCompanyInfo() {
        return findAll().stream().findFirst().orElse(new Company());
    }
}