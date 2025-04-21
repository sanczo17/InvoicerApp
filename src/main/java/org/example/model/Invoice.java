package org.example.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.example.model.enums.InvoiceStatus;
import org.example.model.enums.PaymentMethod;

@Entity
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String invoiceNumber;

    @NotNull(message = "Data wystawienia jest wymagana")
    private LocalDate issueDate = LocalDate.now();

    @NotNull(message = "Termin płatności jest wymagany")
    private LocalDate dueDate = LocalDate.now().plusDays(14);

    @Enumerated(EnumType.STRING)
    @Convert(converter = PaymentMethod.PaymentMethodConverter.class)
    private PaymentMethod paymentMethod = PaymentMethod.PRZELEW;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @NotNull(message = "Status jest wymagany")
    @Enumerated(EnumType.STRING)
    @Convert(converter = InvoiceStatus.InvoiceStatusConverter.class)
    private InvoiceStatus status = InvoiceStatus.NIEOPLACONA;

    private String notes;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItem> items = new ArrayList<>();

    public Invoice() {
    }

    // Preinicjalizacja wartości
    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (status == null) {
            status = InvoiceStatus.NIEOPLACONA;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public InvoiceStatus getStatus() {
        // Dodatkowe zabezpieczenie przed nullem
        if (status == null) {
            return InvoiceStatus.NIEOPLACONA;
        }
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status != null ? status : InvoiceStatus.NIEOPLACONA;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<InvoiceItem> getItems() {
        return items;
    }

    public void setItems(List<InvoiceItem> items) {
        this.items = items;
    }

    public void addItem(InvoiceItem item) {
        items.add(item);
        item.setInvoice(this);
    }

    public void removeItem(InvoiceItem item) {
        items.remove(item);
        item.setInvoice(null);
    }

    public double getTotal() {
        return items.stream().mapToDouble(InvoiceItem::getTotal).sum();
    }

    public boolean isOverdue() {
        return status == InvoiceStatus.NIEOPLACONA && LocalDate.now().isAfter(dueDate);
    }
}