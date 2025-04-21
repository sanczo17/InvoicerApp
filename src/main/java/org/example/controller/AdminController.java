package org.example.controller;

import org.example.model.User;
import org.example.model.enums.InvoiceStatus;
import org.example.service.CustomerService;
import org.example.service.InvoiceService;
import org.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Kontroler obsługujący panel administratora.
 * Zawiera metody zarządzania systemem dostępne tylko dla użytkowników z rolą ADMIN.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")  // Zabezpiecza wszystkie metody w kontrolerze - tylko ADMIN
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final UserService userService;
    private final InvoiceService invoiceService;
    private final CustomerService customerService;

    @Autowired
    public AdminController(UserService userService, InvoiceService invoiceService, CustomerService customerService) {
        this.userService = userService;
        this.invoiceService = invoiceService;
        this.customerService = customerService;
    }

    /**
     * Wyświetla panel administracyjny ze statystykami.
     * Zbiera informacje o użytkownikach, fakturach i klientach.
     */
    @GetMapping
    public String adminPanel(Model model) {
        // Pobieranie danych o użytkownikach
        List<User> users = userService.findAll();
        long activeUsers = users.stream().filter(User::isActive).count();
        long inactiveUsers = users.size() - activeUsers;

        // Pobieranie danych o fakturach
        long totalInvoices = invoiceService.findAll().size();
        long paidInvoices = invoiceService.findByStatus(InvoiceStatus.OPLACONA).size();
        long unpaidInvoices = invoiceService.findByStatus(InvoiceStatus.NIEOPLACONA).size();

        // Pobieranie danych o klientach
        long totalCustomers = customerService.findAll().size();

        // Dodawanie danych do modelu
        model.addAttribute("totalUsers", users.size());
        model.addAttribute("activeUsers", activeUsers);
        model.addAttribute("inactiveUsers", inactiveUsers);

        model.addAttribute("totalInvoices", totalInvoices);
        model.addAttribute("paidInvoices", paidInvoices);
        model.addAttribute("unpaidInvoices", unpaidInvoices);

        model.addAttribute("totalCustomers", totalCustomers);

        return "admin/dashboard";
    }

    /**
     * Nadaje uprawnienia administratora wybranemu użytkownikowi.
     * Jeśli userId nie jest podane, przekierowuje do listy użytkowników.
     */
    @GetMapping("/roles/grant-admin")
    public String grantAdminRole(@RequestParam(required = false) Long userId, RedirectAttributes redirectAttributes) {
        if (userId == null) {
            // Jeśli ID nie zostało podane, przekieruj do listy użytkowników
            return "redirect:/admin/users";
        }

        try {
            logger.info("Próba nadania uprawnień administratora dla użytkownika ID: {}", userId);
            userService.grantAdminRole(userId);
            redirectAttributes.addFlashAttribute("message", "Przyznano uprawnienia administratora");
        } catch (Exception e) {
            logger.error("Błąd podczas nadawania uprawnień administratora: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Wystąpił błąd: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * Odbiera uprawnienia administratora wybranemu użytkownikowi.
     * Jeśli userId nie jest podane, przekierowuje do listy użytkowników.
     */
    @GetMapping("/roles/revoke-admin")
    public String revokeAdminRole(@RequestParam(required = false) Long userId, RedirectAttributes redirectAttributes) {
        if (userId == null) {
            // Jeśli ID nie zostało podane, przekieruj do listy użytkowników
            return "redirect:/admin/users";
        }

        try {
            logger.info("Próba odebrania uprawnień administratora dla użytkownika ID: {}", userId);
            userService.revokeAdminRole(userId);
            logger.info("Pomyślnie odebrano uprawnienia administratora dla użytkownika ID: {}", userId);
            redirectAttributes.addFlashAttribute("message", "Odebrano uprawnienia administratora");
        } catch (Exception e) {
            logger.error("Błąd podczas odbierania uprawnień administratora: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Wystąpił błąd: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * Tworzy kopię zapasową systemu (w rzeczywistej implementacji).
     */
    @GetMapping("/system/backup")
    public String backupSystem(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("message", "Kopia zapasowa została utworzona");
        return "redirect:/admin";
    }

    /**
     * Wyświetla logi systemowe (w rzeczywistej implementacji).
     */
    @GetMapping("/system/logs")
    public String showLogs(Model model) {
        model.addAttribute("logs", "Przykładowe logi systemowe..."); // W rzeczywistości tutaj byłyby pobierane prawdziwe logi
        return "admin/logs";
    }
}