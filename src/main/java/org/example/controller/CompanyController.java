package org.example.controller;

import jakarta.validation.Valid;
import org.example.model.Company;
import org.example.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Kontroler obsługujący operacje na danych firmy.
 * Dostęp tylko dla administratorów.
 */
@Controller
@RequestMapping("/admin/company")
@PreAuthorize("hasRole('ADMIN')") // Tylko administrator może edytować dane firmy
public class CompanyController {

    private final CompanyService companyService;

    @Autowired
    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    /**
     * Wyświetla formularz edycji danych firmy.
     */
    @GetMapping
    public String showCompanyForm(Model model) {
        Company company = companyService.getCompanyInfo();
        model.addAttribute("company", company);
        return "admin/company-form"; // Upewnij się, że ten widok istnieje
    }

    /**
     * Obsługuje zapisywanie danych firmy.
     */
    @PostMapping
    public String saveCompany(@Valid @ModelAttribute("company") Company company,
                              BindingResult result,
                              RedirectAttributes redirectAttributes) {
        // Sprawdzenie błędów walidacji
        if (result.hasErrors()) {
            return "admin/company-form";
        }

        try {
            companyService.saveCompanyInfo(company);
            redirectAttributes.addFlashAttribute("message", "Dane firmy zostały zaktualizowane");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Wystąpił błąd: " + e.getMessage());
        }

        return "redirect:/admin/company"; // Przekierowanie z powrotem do formularza po zapisaniu
    }
}