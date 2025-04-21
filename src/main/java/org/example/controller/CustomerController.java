package org.example.controller;

import org.example.model.Customer;
import org.example.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Kontroler obsługujący operacje na klientach.
 * Odpowiada za zarządzanie danymi klientów w systemie.
 */
@Controller
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;

    @Autowired
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * Wyświetla listę wszystkich klientów.
     */
    @GetMapping
    public String getAllCustomers(Model model) {
        List<Customer> customers = customerService.findAll();
        model.addAttribute("customers", customers);
        return "customer-list";
    }

    /**
     * Wyświetla formularz do tworzenia nowego klienta.
     */
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("customer", new Customer());
        return "customer-form";
    }

    /**
     * Wyświetla formularz do edycji istniejącego klienta.
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Customer customer = customerService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Nieprawidłowe ID klienta: " + id));
            model.addAttribute("customer", customer);
            return "customer-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Wystąpił błąd: " + e.getMessage());
            return "redirect:/customers";
        }
    }

    /**
     * Obsługuje tworzenie nowego lub aktualizację istniejącego klienta.
     */
    @PostMapping
    public String createOrUpdateCustomer(@Valid @ModelAttribute Customer customer,
                                         BindingResult result,
                                         RedirectAttributes redirectAttributes) {
        // Sprawdzenie błędów walidacji
        if (result.hasErrors()) {
            return "customer-form";
        }

        try {
            customerService.save(customer);
            redirectAttributes.addFlashAttribute("message",
                    (customer.getId() == null) ? "Klient został dodany" : "Klient został zaktualizowany");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Wystąpił błąd: " + e.getMessage());
        }

        return "redirect:/customers";
    }

    /**
     * Usuwa klienta o podanym identyfikatorze.
     */
    @GetMapping("/delete/{id}")
    public String deleteCustomer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            customerService.deleteById(id);
            redirectAttributes.addFlashAttribute("message", "Klient został usunięty");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Nie można usunąć klienta: " + e.getMessage());
        }
        return "redirect:/customers";
    }

    /**
     * Pobiera dane klienta w formacie JSON dla żądań AJAX.
     */
    @GetMapping("/get/{id}")
    @ResponseBody
    public ResponseEntity<Customer> getCustomer(@PathVariable Long id) {
        return customerService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}