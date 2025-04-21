package org.example.controller;

import jakarta.validation.Valid;
import org.example.model.User;
import org.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Kontroler zarządzania użytkownikami dostępny tylko dla administratorów.
 * Pozwala na zarządzanie kontami użytkowników w systemie.
 */
@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")  // Dostęp tylko dla administratorów
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Wyświetla listę wszystkich użytkowników.
     */
    @GetMapping
    public String listUsers(Model model) {
        List<User> users = userService.findAll();
        model.addAttribute("users", users);
        return "admin/user-list";
    }

    /**
     * Wyświetla formularz edycji użytkownika.
     */
    @GetMapping("/edit/{id}")
    public String editUserForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findById(id);
            model.addAttribute("user", user);
            return "admin/user-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Wystąpił błąd: " + e.getMessage());
            return "redirect:/admin/users";
        }
    }


    /**
     * Obsługuje aktualizację danych użytkownika.
     */
    @PostMapping("/update")
    public String updateUser(@Valid @ModelAttribute("user") User user,
                             BindingResult result,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/user-form";
        }

        try {
            userService.updateUser(user);
            redirectAttributes.addFlashAttribute("message", "Użytkownik został zaktualizowany");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Wystąpił błąd: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }

    /**
     * Usuwa użytkownika z systemu.
     */
    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("message", "Użytkownik został usunięty");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Nie można usunąć użytkownika: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * Przełącza status aktywności użytkownika.
     */
    @GetMapping("/toggle-status/{id}")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.toggleUserActiveStatus(id);
            redirectAttributes.addFlashAttribute("message", "Status użytkownika został zmieniony");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Wystąpił błąd: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
}