package org.example.controller;

import org.example.model.User;
import org.example.model.enums.InvoiceStatus;
import org.example.service.BackupService;
import org.example.service.CustomerService;
import org.example.service.InvoiceService;
import org.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final BackupService backupService;

    @Autowired
    public AdminController(UserService userService,
                           InvoiceService invoiceService,
                           CustomerService customerService,
                           BackupService backupService) {
        this.userService = userService;
        this.invoiceService = invoiceService;
        this.customerService = customerService;
        this.backupService = backupService;
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
     * Tworzy kopię zapasową systemu.
     * Używa BackupService do eksportu danych do pliku JSON.
     * Wyświetla listę dostępnych kopii zapasowych.
     * Usuwa kopie zapasowe.
     */
    @GetMapping("/system/backups/create")
    public String createBackup(RedirectAttributes redirectAttributes) {
        try {
            String backupPath = backupService.createBackup();
            redirectAttributes.addFlashAttribute("message",
                    "Kopia zapasowa została utworzona: " + backupPath);
            logger.info("Utworzono kopię zapasową: {}", backupPath);
        } catch (Exception e) {
            logger.error("Błąd podczas tworzenia kopii zapasowej: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error",
                    "Wystąpił błąd podczas tworzenia kopii zapasowej: " + e.getMessage());
        }
        return "redirect:/admin/system/backups";
    }

    @GetMapping("/system/backups")
    public String showBackups(Model model) {
        try {
            Path backupDir = Paths.get("backups");
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
            }

            List<Map<String, Object>> backupFiles = new ArrayList<>();
            Files.list(backupDir)
                    .filter(path -> path.toString().endsWith(".json"))
                    .sorted((p1, p2) -> {
                        try {
                            return Files.getLastModifiedTime(p2).compareTo(Files.getLastModifiedTime(p1));
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .forEach(path -> {
                        try {
                            Map<String, Object> fileInfo = new HashMap<>();
                            fileInfo.put("name", path.getFileName().toString());
                            fileInfo.put("size", Files.size(path) / 1024 + " KB");
                            fileInfo.put("date", Files.getLastModifiedTime(path).toString());
                            fileInfo.put("path", path.toString());
                            backupFiles.add(fileInfo);
                        } catch (IOException e) {
                            logger.error("Błąd podczas pobierania informacji o pliku: {}", e.getMessage(), e);
                        }
                    });

            model.addAttribute("backupFiles", backupFiles);
            return "admin/backups";
        } catch (IOException e) {
            logger.error("Błąd podczas listowania plików kopii zapasowych: {}", e.getMessage(), e);
            model.addAttribute("error", "Wystąpił błąd podczas listowania plików kopii zapasowych: " + e.getMessage());
            return "admin/backups";
        }
    }

    /**
     * Pobiera plik kopii zapasowej.
     */
    @GetMapping("/system/backups/download")
    public ResponseEntity<Resource> downloadBackup(@RequestParam String fileName) {
        try {
            Path filePath = Paths.get("backups", fileName);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(resource);
            } else {
                throw new RuntimeException("Nie można znaleźć pliku: " + fileName);
            }
        } catch (Exception e) {
            logger.error("Błąd podczas pobierania pliku kopii zapasowej: {}", e.getMessage(), e);
            throw new RuntimeException("Błąd podczas pobierania pliku: " + e.getMessage());
        }
    }

    /**
     * Przywraca dane z wybranej kopii zapasowej.
     */
    @PostMapping("/system/backups/restore")
    public String restoreBackup(@RequestParam String fileName, RedirectAttributes redirectAttributes) {
        try {
            String message = backupService.restoreFromBackup(fileName);
            redirectAttributes.addFlashAttribute("message", message);
            logger.info("Pomyślnie przywrócono dane z kopii zapasowej: {}", fileName);
        } catch (Exception e) {
            logger.error("Błąd podczas przywracania danych z kopii zapasowej: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Wystąpił błąd podczas przywracania danych: " + e.getMessage());
        }
        return "redirect:/admin/system/backups";
    }

    /**
     * Usuwa wybraną kopię zapasową.
     */
    @PostMapping("/system/backups/delete")
    public String deleteBackup(@RequestParam String fileName, RedirectAttributes redirectAttributes) {
        try {
            Path backupPath = Paths.get("backups", fileName);
            if (Files.exists(backupPath)) {
                Files.delete(backupPath);
                redirectAttributes.addFlashAttribute("message", "Kopia zapasowa została usunięta: " + fileName);
                logger.info("Usunięto kopię zapasową: {}", fileName);
            } else {
                redirectAttributes.addFlashAttribute("error", "Plik kopii zapasowej nie istnieje: " + fileName);
                logger.warn("Próba usunięcia nieistniejącej kopii zapasowej: {}", fileName);
            }
        } catch (Exception e) {
            logger.error("Błąd podczas usuwania kopii zapasowej: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Wystąpił błąd podczas usuwania kopii zapasowej: " + e.getMessage());
        }
        return "redirect:/admin/system/backups";
    }

    /**
     * Wyświetla logi systemowe.
     * Odczytuje ostatnie linie z pliku logu.
     */
    @GetMapping("/system/logs")
    public String showLogs(Model model) {
        try {
            Path logPath = Paths.get("logs/invoicer-app.log");
            if (Files.exists(logPath)) {
                // Odczyt ostatnich 500 linii z pliku logu
                try (Stream<String> lines = Files.lines(logPath)) {
                    List<String> lastLines = lines
                            .skip(Math.max(0, Files.lines(logPath).count() - 500))
                            .collect(Collectors.toList());
                    model.addAttribute("logs", String.join("\n", lastLines));
                }
            } else {
                model.addAttribute("logs", "Plik logu nie istnieje: " + logPath.toAbsolutePath());
                logger.warn("Plik logu nie istnieje: {}", logPath.toAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("Błąd podczas odczytu pliku logu: {}", e.getMessage(), e);
            model.addAttribute("logs", "Wystąpił błąd podczas odczytu pliku logu: " + e.getMessage());
        }
        return "admin/logs";
    }
}