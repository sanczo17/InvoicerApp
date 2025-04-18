package org.example.controller;

import jakarta.validation.Valid;
import org.example.model.Invoice;
import org.example.model.InvoiceItem;
import org.example.repository.InvoiceRepository;
import org.example.model.InvoiceStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
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
    public String createOrUpdate(@ModelAttribute Invoice invoice, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("statuses", InvoiceStatus.values());
            return "invoice-form";
        }

        // Usuń puste pozycje
        invoice.getItems().removeIf(item -> item.getProduct() == null || item.getProduct().isEmpty());

        // Ustaw relację dla wszystkich elementów
        for (InvoiceItem item : invoice.getItems()) {
            item.setInvoice(invoice);
        }

        invoiceRepository.save(invoice);
        return "redirect:/invoices";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nieprawidłowe ID faktury: " + id));

        // Jeśli faktura nie ma pozycji, dodaj jedną pustą
        if (invoice.getItems().isEmpty()) {
            invoice.getItems().add(new InvoiceItem());
        }

        model.addAttribute("invoice", invoice);
        model.addAttribute("statuses", InvoiceStatus.values());
        return "invoice-form";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            invoiceRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("message", "Faktura została usunięta");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Nie można usunąć faktury");
        }
        return "redirect:/invoices";
    }

    @PostMapping("/addItem")
    public String addItem(@ModelAttribute Invoice invoice, Model model) {
        invoice.getItems().add(new InvoiceItem());
        model.addAttribute("invoice", invoice);
        model.addAttribute("statuses", InvoiceStatus.values());
        return "invoice-form";
    }

    @GetMapping("/removeItem/{index}")
    public String removeItem(@ModelAttribute Invoice invoice, @PathVariable int index, Model model) {
        if (index >= 0 && index < invoice.getItems().size()) {
            invoice.getItems().remove(index);
        }

        // Upewnij się, że zawsze jest przynajmniej jedna pozycja
        if (invoice.getItems().isEmpty()) {
            invoice.getItems().add(new InvoiceItem());
        }

        model.addAttribute("invoice", invoice);
        model.addAttribute("statuses", InvoiceStatus.values());
        return "invoice-form";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleException(IllegalArgumentException exception, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", exception.getMessage());
        return "redirect:/invoices";
    }
}