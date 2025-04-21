package org.example.controller;

import jakarta.validation.Valid;
import org.example.model.PasswordChangeForm;
import org.example.model.User;
import org.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("user", new User());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerSubmit(@Valid @ModelAttribute User user,
                                 BindingResult result,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "auth/register";
        }

        try {
            if (userService.existsByUsername(user.getUsername())) {
                model.addAttribute("usernameError", "Nazwa użytkownika jest już zajęta");
                return "auth/register";
            }

            if (userService.existsByEmail(user.getEmail())) {
                model.addAttribute("emailError", "Adres email jest już zajęty");
                return "auth/register";
            }

            userService.registerNewUser(user);

            redirectAttributes.addFlashAttribute("message",
                    "Rejestracja przebiegła pomyślnie. Możesz się teraz zalogować.");
            return "redirect:/auth/login";

        } catch (Exception e) {
            model.addAttribute("error", "Wystąpił błąd podczas rejestracji: " + e.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/change-password")
    public String showChangePasswordForm(Model model) {
        // Sprawdź czy użytkownik jest zalogowany
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "redirect:/auth/login";
        }

        model.addAttribute("passwordForm", new PasswordChangeForm());
        return "auth/change-password";
    }

    @PostMapping("/perform-change-password")
    public String processChangePassword(@Valid @ModelAttribute("passwordForm") PasswordChangeForm form,
                                        BindingResult result,
                                        RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "redirect:/auth/login";
        }

        if (result.hasErrors()) {
            return "auth/change-password";
        }

        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.passwordForm", "Hasła nie są zgodne");
            return "auth/change-password";
        }

        try {
            userService.changePassword(auth.getName(), form.getNewPassword());

            SecurityContextHolder.clearContext();

            redirectAttributes.addFlashAttribute("message", "Twoje hasło zostało zmienione. Zaloguj się ponownie.");
            return "redirect:/auth/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Wystąpił błąd: " + e.getMessage());
            return "redirect:/auth/change-password";
        }
    }
}