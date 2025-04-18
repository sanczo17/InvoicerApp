package org.example.controller;

import org.example.model.Invoice;
import org.example.model.InvoiceItem;
import org.example.repository.InvoiceRepository;
import org.example.model.InvoiceStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/invoices")
public class InvoiceController {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @GetMapping
    public String getAll(@RequestParam(required = false) InvoiceStatus status, Model model) {
        List<Invoice> invoices = (status == null) ?
                invoiceRepository.findAll() :
                invoiceRepository.findByStatus(status);

        double total = invoices.stream().mapToDouble(Invoice::getTotal).sum();
        model.addAttribute("invoices", invoices);
        model.addAttribute("summary", total);
        model.addAttribute("statusFilter", status);
        return "invoice-list";
    }

    @GetMapping("/new")
    public String showForm(Model model) {
        Invoice invoice = new Invoice();
        invoice.getItems().add(new InvoiceItem());  // Dodaj pierwszą pustą pozycję

        model.addAttribute("invoice", invoice);
        model.addAttribute("statuses", InvoiceStatus.values());
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

            for (InvoiceItem item : invoice.getItems()) {
                item.setInvoice(invoice);
            }

            invoiceRepository.save(invoice);
            redirectAttributes.addFlashAttribute("message", "Faktura została zapisana");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Wystąpił błąd podczas zapisywania faktury: " + e.getMessage());
        }
        return "redirect:/invoices";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Invoice invoice = invoiceRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Nieprawidłowe ID faktury: " + id));

            if (invoice.getItems().isEmpty()) {
                invoice.getItems().add(new InvoiceItem());
            }

            model.addAttribute("invoice", invoice);
            model.addAttribute("statuses", InvoiceStatus.values());
            return "invoice-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Wystąpił błąd: " + e.getMessage());
            return "redirect:/invoices";
        }
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            invoiceRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("message", "Faktura została usunięta");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Nie można usunąć faktury: " + e.getMessage());
        }
        return "redirect:/invoices";
    }
}