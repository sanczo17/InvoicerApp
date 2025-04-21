package org.example.controller;

import org.example.model.Invoice;
import org.example.model.InvoiceItem;
import org.example.model.enums.InvoiceStatus;
import org.example.model.enums.PaymentMethod;
import org.example.service.InvoiceService;
import org.example.service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.beans.PropertyEditorSupport;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final PdfService pdfService;

    @Autowired
    public InvoiceController(InvoiceService invoiceService, PdfService pdfService) {
        this.invoiceService = invoiceService;
        this.pdfService = pdfService;
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
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

    @GetMapping("/new")
    public String showForm(Model model) {
        Invoice invoice = new Invoice();
        invoice.getItems().add(new InvoiceItem());

        model.addAttribute("invoice", invoice);
        model.addAttribute("statuses", InvoiceStatus.values());
        model.addAttribute("paymentMethods", PaymentMethod.values());
        return "invoice-form";
    }

    @PostMapping
    public String createOrUpdate(@ModelAttribute Invoice invoice, RedirectAttributes redirectAttributes) {
        try {
            invoice.getItems().removeIf(item -> item.getProduct() == null || item.getProduct().isEmpty());

            if (invoice.getItems().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Faktura musi mieć co najmniej jedną pozycję");
                return "redirect:/invoices/new";
            }

            invoiceService.save(invoice);
            redirectAttributes.addFlashAttribute("message", "Faktura została zapisana");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Wystąpił błąd podczas zapisywania faktury: " + e.getMessage());
        }
        return "redirect:/invoices";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Invoice invoice = invoiceService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Nieprawidłowe ID faktury: " + id));

            if (invoice.getItems().isEmpty()) {
                invoice.getItems().add(new InvoiceItem());
            }

            model.addAttribute("invoice", invoice);
            model.addAttribute("statuses", InvoiceStatus.values());
            model.addAttribute("paymentMethods", PaymentMethod.values());
            return "invoice-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Wystąpił błąd: " + e.getMessage());
            return "redirect:/invoices";
        }
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            invoiceService.deleteById(id);
            redirectAttributes.addFlashAttribute("message", "Faktura została usunięta");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Nie można usunąć faktury: " + e.getMessage());
        }
        return "redirect:/invoices";
    }

    @GetMapping("/pdf/{id}")
    public ResponseEntity<byte[]> generatePdf(@PathVariable Long id) {
        try {
            Invoice invoice = invoiceService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Nieprawidłowe ID faktury: " + id));

            byte[] pdfContent = pdfService.generateInvoicePdf(invoice);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "faktura_" + invoice.getInvoiceNumber() + ".pdf";
            headers.setContentDispositionFormData(filename, filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/search")
    public String searchInvoices(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            Model model) {

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
    }
}