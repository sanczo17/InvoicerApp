package org.example.controller;

import org.example.exception.ResourceNotFoundException;
import org.example.model.Customer;
import org.example.model.Invoice;
import org.example.model.InvoiceItem;
import org.example.model.enums.InvoiceStatus;
import org.example.model.enums.PaymentMethod;
import org.example.service.CustomerService;
import org.example.service.InvoiceService;
import org.example.service.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.beans.PropertyEditorSupport;
import java.time.LocalDate;
import java.util.List;

/**
 * Kontroler obsługujący operacje na fakturach.
 */
@Controller
@RequestMapping("/invoices")
public class InvoiceController {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceController.class);

    private final InvoiceService invoiceService;
    private final PdfService pdfService;
    private final CustomerService customerService;

    @Autowired
    public InvoiceController(InvoiceService invoiceService, PdfService pdfService, CustomerService customerService) {
        this.invoiceService = invoiceService;
        this.pdfService = pdfService;
        this.customerService = customerService;
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // Rejestracja konwerterów dla enumów
        binder.registerCustomEditor(PaymentMethod.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                try {
                    setValue(PaymentMethod.valueOf(text));
                } catch (IllegalArgumentException e) {
                    try {
                        setValue(PaymentMethod.fromDisplayName(text));
                    } catch (IllegalArgumentException ex) {
                        setValue(PaymentMethod.PRZELEW);
                    }
                }
            }
        });

        binder.registerCustomEditor(InvoiceStatus.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                try {
                    setValue(InvoiceStatus.valueOf(text));
                } catch (IllegalArgumentException e) {
                    try {
                        for (InvoiceStatus status : InvoiceStatus.values()) {
                            if (status.getDisplayName().equals(text)) {
                                setValue(status);
                                return;
                            }
                        }
                        setValue(InvoiceStatus.NIEOPLACONA);
                    } catch (Exception ex) {
                        setValue(InvoiceStatus.NIEOPLACONA);
                    }
                }
            }
        });
    }

    /**
     * Wyświetla listę faktur, opcjonalnie filtrowaną po statusie.
     */
    @GetMapping
    public String getAll(@RequestParam(required = false) InvoiceStatus status, Model model) {
        List<Invoice> invoices = (status == null) ?
                invoiceService.findAll() :
                invoiceService.findByStatus(status);

        double total = invoices.stream().mapToDouble(Invoice::getTotal).sum();
        model.addAttribute("invoices", invoices);
        model.addAttribute("summary", total);
        model.addAttribute("statusFilter", status);
        return "invoice-list";
    }

    /**
     * Wyświetla formularz tworzenia nowej faktury.
     */
    @GetMapping("/new")
    public String showForm(Model model) {
        Invoice invoice = new Invoice();
        invoice.getItems().add(new InvoiceItem());

        // Pobierz listę wszystkich klientów
        List<Customer> customers = customerService.findAll();

        model.addAttribute("invoice", invoice);
        model.addAttribute("customers", customers);
        model.addAttribute("statuses", InvoiceStatus.values());
        model.addAttribute("paymentMethods", PaymentMethod.values());
        return "invoice-form";
    }

    /**
     * Obsługuje zapis lub aktualizację faktury.
     */
    @PostMapping
    public String createOrUpdate(@Valid @ModelAttribute Invoice invoice,
                                 BindingResult result,
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "invoice-form";
        }

        try {
            // Usuń puste pozycje faktury
            invoice.getItems().removeIf(item -> item.getProduct() == null || item.getProduct().isEmpty());

            if (invoice.getItems().isEmpty()) {
                throw new IllegalArgumentException("Faktura musi mieć co najmniej jedną pozycję");
            }

            invoiceService.save(invoice);
            redirectAttributes.addFlashAttribute("message", "Faktura została zapisana");
            return "redirect:/invoices";

        } catch (IllegalArgumentException e) {
            logger.warn("Błąd walidacji faktury: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/invoices/new";
        } catch (Exception e) {
            logger.error("Nieoczekiwany błąd podczas zapisywania faktury: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Wystąpił błąd podczas zapisywania faktury: " + e.getMessage());
            return "redirect:/invoices";
        }
    }

    /**
     * Wyświetla formularz edycji faktury.
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Invoice invoice = invoiceService.findById(id);

            if (invoice.getItems().isEmpty()) {
                invoice.getItems().add(new InvoiceItem());
            }

            // Pobierz listę wszystkich klientów
            List<Customer> customers = customerService.findAll();

            model.addAttribute("invoice", invoice);
            model.addAttribute("customers", customers);
            model.addAttribute("statuses", InvoiceStatus.values());
            model.addAttribute("paymentMethods", PaymentMethod.values());
            return "invoice-form";
        } catch (ResourceNotFoundException e) {
            logger.warn("Próba edycji nieistniejącej faktury: {}", id);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/invoices";
        } catch (Exception e) {
            logger.error("Nieoczekiwany błąd podczas edycji faktury: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Wystąpił błąd: " + e.getMessage());
            return "redirect:/invoices";
        }
    }

    /**
     * Usuwa fakturę.
     */
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            invoiceService.deleteById(id);
            redirectAttributes.addFlashAttribute("message", "Faktura została usunięta");
        } catch (ResourceNotFoundException e) {
            logger.warn("Próba usunięcia nieistniejącej faktury: {}", id);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            logger.error("Nieoczekiwany błąd podczas usuwania faktury: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Nie można usunąć faktury: " + e.getMessage());
        }
        return "redirect:/invoices";
    }

    /**
     * Generuje PDF faktury.
     */
    @GetMapping("/pdf/{id}")
    public ResponseEntity<byte[]> generatePdf(@PathVariable Long id) {
        try {
            Invoice invoice = invoiceService.findById(id);
            byte[] pdfContent = pdfService.generateInvoicePdf(invoice);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "faktura_" + invoice.getInvoiceNumber() + ".pdf";
            headers.setContentDispositionFormData(filename, filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            logger.warn("Próba wygenerowania PDF dla nieistniejącej faktury: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Błąd podczas generowania PDF dla faktury: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Wyszukuje faktury według podanych kryteriów.
     */
    @GetMapping("/search")
    public String searchInvoices(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            Model model) {

        try {
            List<Invoice> invoices = invoiceService.searchInvoices(
                    status, startDate, endDate, customerName, minAmount, maxAmount);

            double total = invoices.stream().mapToDouble(Invoice::getTotal).sum();
            model.addAttribute("invoices", invoices);
            model.addAttribute("summary", total);
            model.addAttribute("statusFilter", status);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("customerName", customerName);
            model.addAttribute("minAmount", minAmount);
            model.addAttribute("maxAmount", maxAmount);

            return "invoice-list";
        } catch (Exception e) {
            logger.error("Błąd podczas wyszukiwania faktur: {}", e.getMessage(), e);
            model.addAttribute("error", "Błąd podczas wyszukiwania faktur: " + e.getMessage());
            return "invoice-list";
        }
    }
}