package org.example.service;

import org.example.model.Customer;
import org.example.model.Invoice;
import org.example.model.InvoiceItem;
import org.example.model.enums.InvoiceStatus;
import org.example.repository.CustomerRepository;
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
    private final CustomerRepository customerRepository;

    @Autowired
    public InvoiceService(InvoiceRepository invoiceRepository, CustomerRepository customerRepository) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
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
        if (invoice.getCustomer() != null && invoice.getCustomer().getId() != null) {
            Customer existingCustomer = customerRepository.findById(invoice.getCustomer().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Nieprawidłowe ID klienta: " + invoice.getCustomer().getId()));
            invoice.setCustomer(existingCustomer);
        }

        if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isEmpty()) {
            invoice.setInvoiceNumber(generateInvoiceNumber(invoice));
        }

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

    public List<Invoice> findOverdueinvoices() {
        LocalDate today = LocalDate.now();
        return invoiceRepository.findAll().stream()
                .filter(invoice -> invoice.getStatus() == InvoiceStatus.NIEOPLACONA &&
                        invoice.getDueDate().isBefore(today))
                .toList();
    }

    public List<Invoice> findInvoicesForMonth(int year, int month) {
        return invoiceRepository.findAll().stream()
                .filter(invoice -> invoice.getIssueDate() != null &&
                        invoice.getIssueDate().getYear() == year &&
                        invoice.getIssueDate().getMonthValue() == month)
                .toList();
    }
}