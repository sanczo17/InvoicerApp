package org.example.service;

import org.example.model.Invoice;
import org.example.model.InvoiceItem;
import org.example.model.enums.InvoiceStatus;
import org.example.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    @Autowired
    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public List<Invoice> findAll() {
        return invoiceRepository.findAll();
    }

    public List<Invoice> findByStatus(InvoiceStatus status) {
        return invoiceRepository.findByStatus(status);
    }

    public Optional<Invoice> findById(Long id) {
        return invoiceRepository.findById(id);
    }

    @Transactional
    public Invoice save(Invoice invoice) {
        // Generuj numer faktury, jeśli nie został podany
        if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isEmpty()) {
            invoice.setInvoiceNumber(generateInvoiceNumber(invoice));
        }

        // Ustaw powiązanie między fakturą a jej pozycjami
        if (invoice.getItems() != null) {
            for (InvoiceItem item : invoice.getItems()) {
                item.setInvoice(invoice);
            }
        }

        return invoiceRepository.save(invoice);
    }

    @Transactional
    public void deleteById(Long id) {
        invoiceRepository.deleteById(id);
    }

    public List<Invoice> searchInvoices(
            InvoiceStatus status,
            LocalDate startDate,
            LocalDate endDate,
            String customerName,
            Double minAmount,
            Double maxAmount) {
        return invoiceRepository.searchInvoices(status, startDate, endDate, customerName, minAmount, maxAmount);
    }

    // Generuje numer faktury w formacie FV/YYYY/MM/XX
    private String generateInvoiceNumber(Invoice invoice) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM");
        String datePart = invoice.getIssueDate().format(formatter);

        // Zlicz istniejące faktury z tego samego miesiąca/roku
        long count = invoiceRepository.findAll().stream()
                .filter(inv -> inv.getIssueDate() != null &&
                        inv.getIssueDate().getYear() == invoice.getIssueDate().getYear() &&
                        inv.getIssueDate().getMonth() == invoice.getIssueDate().getMonth())
                .count();

        return String.format("FV/%s/%02d", datePart, count + 1);
    }

    // Zwraca faktury przeterminowane
    public List<Invoice> findOverdueinvoices() {
        LocalDate today = LocalDate.now();
        return invoiceRepository.findAll().stream()
                .filter(invoice -> invoice.getStatus() == InvoiceStatus.NIEOPLACONA &&
                        invoice.getDueDate().isBefore(today))
                .toList();
    }

    // Zwraca faktury wystawione w danym miesiącu
    public List<Invoice> findInvoicesForMonth(int year, int month) {
        return invoiceRepository.findAll().stream()
                .filter(invoice -> invoice.getIssueDate() != null &&
                        invoice.getIssueDate().getYear() == year &&
                        invoice.getIssueDate().getMonthValue() == month)
                .toList();
    }
}