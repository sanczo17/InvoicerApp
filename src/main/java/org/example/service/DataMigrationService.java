package org.example.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.example.model.Invoice;
import org.example.model.enums.InvoiceStatus;
import org.example.repository.InvoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class DataMigrationService {

    private static final Logger logger = LoggerFactory.getLogger(DataMigrationService.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private InvoiceRepository invoiceRepository;

    /**
     * Uruchamia się automatycznie po starcie aplikacji, aby naprawić dane w bazie.
     * Można wyłączyć tę metodę, gdy nie jest już potrzebna.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void fixInvoiceStatusesOnStartup() {
        logger.info("Rozpoczynam naprawę danych statusów faktur...");

        try {
            // 1. Najpierw sprawdź schemat bazy danych i ewentualnie go napraw
            checkAndFixDatabaseSchema();

            // 2. Napraw dane w bezpiecznym trybie
            fixInvoiceStatusesSafely();

            logger.info("Naprawa danych statusów faktur zakończona pomyślnie");
        } catch (Exception e) {
            logger.error("Błąd podczas naprawy statusów faktur: {}", e.getMessage(), e);
            // Nie rzucamy wyjątku, aby aplikacja mogła się uruchomić
        }
    }

    @Transactional
    private void checkAndFixDatabaseSchema() {
        try {
            // Sprawdź typ kolumny status
            Query schemaQuery = entityManager.createNativeQuery(
                    "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, COLUMN_TYPE " +
                            "FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'invoice' AND COLUMN_NAME = 'status'"
            );

            List<Object[]> results = schemaQuery.getResultList();
            if (!results.isEmpty()) {
                Object[] row = results.get(0);
                String dataType = (String) row[1];
                Object maxLength = row[2];
                String columnType = (String) row[3];

                logger.info("Kolumna 'status' ma typ: {} z maksymalną długością: {} (pełny typ: {})",
                        dataType, maxLength, columnType);

                // Sprawdź czy kolumna ma wystarczającą długość dla naszych statusów
                if ("varchar".equalsIgnoreCase(dataType) && maxLength != null) {
                    int length = ((Number) maxLength).intValue();
                    if (length < 15) {
                        // Kolumna jest za krótka, musimy ją rozszerzyć
                        logger.info("Rozszerzam kolumnę 'status' do VARCHAR(20)...");
                        Query alterQuery = entityManager.createNativeQuery(
                                "ALTER TABLE invoice MODIFY COLUMN status VARCHAR(20)"
                        );
                        alterQuery.executeUpdate();
                        logger.info("Kolumna 'status' została rozszerzona pomyślnie");
                    }
                } else if ("enum".equalsIgnoreCase(dataType)) {
                    // Kolumna jest typu ENUM, musimy ją zmienić na VARCHAR
                    logger.info("Zmieniam kolumnę 'status' z ENUM na VARCHAR(20)...");
                    Query alterQuery = entityManager.createNativeQuery(
                            "ALTER TABLE invoice MODIFY COLUMN status VARCHAR(20)"
                    );
                    alterQuery.executeUpdate();
                    logger.info("Kolumna 'status' została zmieniona na VARCHAR(20)");
                }
            } else {
                logger.warn("Nie znaleziono informacji o kolumnie 'status' w tabeli 'invoice'");
            }
        } catch (Exception e) {
            logger.error("Błąd podczas sprawdzania/naprawiania schematu bazy danych: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    private void fixInvoiceStatusesSafely() {
        try {
            // 1. Sprawdzamy, jakie statusy mamy w bazie
            Query checkQuery = entityManager.createNativeQuery("SELECT DISTINCT status FROM invoice");
            List<Object> statuses = checkQuery.getResultList();

            logger.info("Znaleziono następujące statusy w bazie: {}", statuses);

            // 2. Naprawiamy NULL i puste wartości (krótszą wartością)
            try {
                Query nullQuery = entityManager.createNativeQuery(
                        "UPDATE invoice SET status = 'OPLACONA' WHERE status IS NULL OR status = ''"
                );
                int nullUpdated = nullQuery.executeUpdate();
                logger.info("Naprawiono {} rekordów z pustymi wartościami", nullUpdated);
            } catch (Exception e) {
                logger.error("Błąd podczas aktualizacji pustych wartości: {}", e.getMessage());
            }

            // 3. Naprawiamy nieprawidłowe wartości z polskimi znakami
            try {
                Query polishQuery1 = entityManager.createNativeQuery(
                        "UPDATE invoice SET status = 'OPLACONA' WHERE status = 'OPŁACONA'"
                );
                int polishUpdated1 = polishQuery1.executeUpdate();
                logger.info("Zaktualizowano {} rekordów z 'OPŁACONA' na 'OPLACONA'", polishUpdated1);
            } catch (Exception e) {
                logger.error("Błąd podczas aktualizacji 'OPŁACONA': {}", e.getMessage());
            }

            try {
                Query polishQuery2 = entityManager.createNativeQuery(
                        "UPDATE invoice SET status = 'NIEOPLACONA' WHERE status = 'NIEOPŁACONA'"
                );
                int polishUpdated2 = polishQuery2.executeUpdate();
                logger.info("Zaktualizowano {} rekordów z 'NIEOPŁACONA' na 'NIEOPLACONA'", polishUpdated2);
            } catch (Exception e) {
                logger.error("Błąd podczas aktualizacji 'NIEOPŁACONA': {}", e.getMessage());
            }

            // 4. Ładujemy wszystkie faktury i upewniamy się, że statusy są poprawne
            List<Invoice> allInvoices = invoiceRepository.findAll();
            int fixedInCode = 0;

            for (Invoice invoice : allInvoices) {
                if (invoice.getStatus() == null) {
                    invoice.setStatus(InvoiceStatus.OPLACONA); // Używamy krótszej wartości
                    fixedInCode++;
                }
            }

            if (fixedInCode > 0) {
                invoiceRepository.saveAll(allInvoices);
                logger.info("Naprawiono {} rekordów na poziomie kodu Java", fixedInCode);
            }
        } catch (Exception e) {
            logger.error("Błąd podczas naprawy danych: {}", e.getMessage(), e);
            throw e;
        }
    }
}