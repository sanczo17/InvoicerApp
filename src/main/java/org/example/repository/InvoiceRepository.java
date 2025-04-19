package org.example.repository;

import org.example.model.Invoice;
import org.example.model.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByStatus(InvoiceStatus status);

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