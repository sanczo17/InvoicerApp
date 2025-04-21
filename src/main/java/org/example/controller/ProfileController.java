package org.example.controller;

import jakarta.validation.Valid;
import org.example.model.PasswordChangeForm;
import org.example.model.User;
import org.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Kontroler obsługujący zarządzanie profilem użytkownika.
 * Pozwala użytkownikowi na zmianę hasła i edycję danych profilu.
 */
@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;

    @Autowired
    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Wyświetla formularz zmiany hasła.
     */
    @GetMapping("/change-password")
    public String showChangePasswordForm(Model model) {
        // Sprawdź czy użytkownik jest zalogowany
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        // Pobierz dane użytkownika
        User user = userService.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika"));

        model.addAttribute("user", user);
        model.addAttribute("passwordForm", new PasswordChangeForm());
        return "profile/change-password";
    }

    /**
     * Obsługuje zmianę hasła przez użytkownika.
     */
    @PostMapping("/change-password")
    public String processChangePassword(@Valid @ModelAttribute("passwordForm") PasswordChangeForm form,
                                        BindingResult result,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        // Sprawdź czy użytkownik jest zalogowany
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        // Pobierz dane użytkownika
        User user = userService.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika"));

        model.addAttribute("user", user);

        // Sprawdź błędy walidacji
        if (result.hasErrors()) {
            return "profile/change-password";
        }

        // Sprawdź zgodność haseł
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.passwordForm", "Hasła nie są zgodne");
            return "profile/change-password";
        }

        try {
            // Zmień hasło użytkownika
            userService.changePassword(auth.getName(), form.getNewPassword());

            redirectAttributes.addFlashAttribute("message", "Twoje hasło zostało zmienione pomyślnie.");
            return "redirect:/";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Wystąpił błąd: " + e.getMessage());
            return "redirect:/profile/change-password";
        }
    }
}