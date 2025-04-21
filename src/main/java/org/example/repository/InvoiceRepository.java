package org.example.repository;

import org.example.model.Invoice;
import org.example.model.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repozytorium dla encji Invoice.
 * Dostarcza metody do operacji na bazie danych dla faktur.
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /**
     * Znajduje faktury o określonym statusie.
     *
     * @param status status faktury do wyszukania
     * @return lista faktur o podanym statusie
     */
    List<Invoice> findByStatus(InvoiceStatus status);

    /**
     * Zaawansowane wyszukiwanie faktur z wieloma kryteriami.
     * Parametry mogą być null, co oznacza brak filtrowania po danym kryterium.
     *
     * @param status opcjonalny status faktury
     * @param startDate opcjonalna minimalna data wystawienia faktury
     * @param endDate opcjonalna maksymalna data wystawienia faktury
     * @param customerName opcjonalna fraza w nazwie klienta
     * @param minAmount opcjonalna minimalna kwota faktury
     * @param maxAmount opcjonalna maksymalna kwota faktury
     * @return lista faktur pasujących do kryteriów
     */
    @Query("SELECT i FROM Invoice i WHERE " +
            "(:status IS NULL OR i.status = :status) AND " +
            "(:startDate IS NULL OR i.issueDate >= :startDate) AND " +
            "(:endDate IS NULL OR i.issueDate <= :endDate) AND " +
            "(:customerName IS NULL OR LOWER(i.customer.name) LIKE LOWER(CONCAT('%', :customerName, '%'))) AND " +
            "(:minAmount IS NULL OR (SELECT SUM(it.quantity * it.price) FROM InvoiceItem it WHERE it.invoice = i) >= :minAmount) AND " +
            "(:maxAmount IS NULL OR (SELECT SUM(it.quantity * it.price) FROM InvoiceItem it WHERE it.invoice = i) <= :maxAmount)")
    List<Invoice> searchInvoices(
            @Param("status") InvoiceStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("customerName") String customerName,
            @Param("minAmount") Double minAmount,
            @Param("maxAmount") Double maxAmount);
}