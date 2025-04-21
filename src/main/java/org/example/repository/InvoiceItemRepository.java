package org.example.repository;

import org.example.model.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repozytorium dla encji InvoiceItem.
 * Dostarcza metody do operacji na bazie danych dla pozycji faktur.
 */
@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {
    // Można dodać tutaj metody specyficzne dla pozycji faktur
    // Domyślne metody jak save(), findById(), findAll() są już dostępne z JpaRepository
}