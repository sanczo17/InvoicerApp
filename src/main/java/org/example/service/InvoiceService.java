package org.example.service;

import org.example.exception.ResourceNotFoundException;
import org.example.model.Customer;
import org.example.model.Invoice;
import org.example.model.InvoiceItem;
import org.example.model.enums.InvoiceStatus;
import org.example.repository.CustomerRepository;
import org.example.repository.InvoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Serwis obsługujący operacje na fakturach.
 * Zawiera logikę biznesową dotyczącą zapisu, wyszukiwania i przetwarzania faktur.
 */
@Service
public class InvoiceService {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceService.class);

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;

    @Autowired
    public InvoiceService(InvoiceRepository invoiceRepository, CustomerRepository customerRepository) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * Pobiera wszystkie faktury.
     */
    public List<Invoice> findAll() {
        logger.debug("Pobieranie wszystkich faktur");
        return invoiceRepository.findAll();
    }

    /**
     * Pobiera faktury według statusu.
     */
    public List<Invoice> findByStatus(InvoiceStatus status) {
        logger.debug("Pobieranie faktur o statusie: {}", status);
        return invoiceRepository.findByStatus(status);
    }

    /**
     * Pobiera fakturę po identyfikatorze.
     *
     * @param id identyfikator faktury
     * @return faktura
     * @throws ResourceNotFoundException gdy faktura o podanym id nie istnieje
     */
    public Invoice findById(Long id) {
        logger.debug("Pobieranie faktury o id: {}", id);
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Faktura", id));
    }

    /**
     * Zapisuje lub aktualizuje fakturę.
     * Jeśli faktura ma przypisanego klienta po id, pobiera tego klienta z bazy.
     * Jeśli faktura nie ma numeru, generuje go automatycznie.
     * Ustawia referencje do faktury dla każdej pozycji faktury.
     */
    @Transactional
    public Invoice save(Invoice invoice) {
        // Logika dla klienta faktury
        if (invoice.getCustomer() != null && invoice.getCustomer().getId() != null) {
            Customer existingCustomer = customerRepository.findById(invoice.getCustomer().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Klient", invoice.getCustomer().getId()));
            invoice.setCustomer(existingCustomer);
        }

        // Generowanie numeru faktury
        if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isEmpty()) {
            invoice.setInvoiceNumber(generateInvoiceNumber(invoice));
        }

        // Ustawienie referencji do faktury dla każdej pozycji
        if (invoice.getItems() != null) {
            for (InvoiceItem item : invoice.getItems()) {
                item.setInvoice(invoice);
            }
        }

        // Sprawdzenie poprawności danych
        validateInvoice(invoice);

        logger.info("Zapisuję fakturę: {}", invoice.getInvoiceNumber());
        return invoiceRepository.save(invoice);
    }

    /**
     * Walidacja podstawowych danych faktury.
     * Sprawdza czy faktura ma poprawną datę wystawienia i termin płatności.
     *
     * @param invoice faktura do walidacji
     * @throws IllegalArgumentException w przypadku nieprawidłowych danych
     */
    private void validateInvoice(Invoice invoice) {
        if (invoice.getIssueDate() == null) {
            throw new IllegalArgumentException("Data wystawienia faktury jest wymagana");
        }

        if (invoice.getDueDate() == null) {
            throw new IllegalArgumentException("Termin płatności faktury jest wymagany");
        }

        if (invoice.getDueDate().isBefore(invoice.getIssueDate())) {
            throw new IllegalArgumentException("Termin płatności nie może być wcześniejszy niż data wystawienia");
        }

        if (invoice.getItems() == null || invoice.getItems().isEmpty()) {
            throw new IllegalArgumentException("Faktura musi zawierać co najmniej jedną pozycję");
        }
    }

    /**
     * Usuwa fakturę o podanym identyfikatorze.
     */
    @Transactional
    public void deleteById(Long id) {
        // Sprawdzamy czy faktura istnieje przed usunięciem
        if (!invoiceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Faktura", id);
        }

        logger.info("Usuwam fakturę o id: {}", id);
        invoiceRepository.deleteById(id);
    }

    /**
     * Wyszukuje faktury według podanych kryteriów.
     */
    public List<Invoice> searchInvoices(
            InvoiceStatus status,
            LocalDate startDate,
            LocalDate endDate,
            String customerName,
            Double minAmount,
            Double maxAmount) {

        logger.debug("Wyszukiwanie faktur z parametrami: status={}, startDate={}, endDate={}, customerName={}, minAmount={}, maxAmount={}",
                status, startDate, endDate, customerName, minAmount, maxAmount);

        return invoiceRepository.searchInvoices(status, startDate, endDate, customerName, minAmount, maxAmount);
    }

    /**
     * Generuje numer faktury w formacie FV/YYYY/MM/XX, gdzie XX to kolejny numer faktury w danym miesiącu.
     */
    private String generateInvoiceNumber(Invoice invoice) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM");
        String datePart = invoice.getIssueDate().format(formatter);

        long count = invoiceRepository.findAll().stream()
                .filter(inv -> inv.getIssueDate() != null &&
                        inv.getIssueDate().getYear() == invoice.getIssueDate().getYear() &&
                        inv.getIssueDate().getMonth() == invoice.getIssueDate().getMonth())
                .count();

        return String.format("FV/%s/%02d", datePart, count + 1);
    }

    /**
     * Znajduje przeterminowane faktury (nieopłacone, których termin płatności minął).
     */
    public List<Invoice> findOverdueInvoices() {
        LocalDate today = LocalDate.now();
        logger.debug("Pobieranie przeterminowanych faktur na dzień: {}", today);

        return invoiceRepository.findAll().stream()
                .filter(invoice -> invoice.getStatus() == InvoiceStatus.NIEOPLACONA &&
                        invoice.getDueDate().isBefore(today))
                .toList();
    }

    /**
     * Znajduje faktury z danego miesiąca i roku.
     */
    public List<Invoice> findInvoicesForMonth(int year, int month) {
        logger.debug("Pobieranie faktur dla roku: {} i miesiąca: {}", year, month);

        return invoiceRepository.findAll().stream()
                .filter(invoice -> invoice.getIssueDate() != null &&
                        invoice.getIssueDate().getYear() == year &&
                        invoice.getIssueDate().getMonthValue() == month)
                .toList();
    }
}