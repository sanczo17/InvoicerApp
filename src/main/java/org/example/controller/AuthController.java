package org.example.controller;

import jakarta.validation.Valid;
import org.example.model.User;
import org.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
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
            // Sprawdź, czy nazwa użytkownika i email są już zajęte
            if (userService.existsByUsername(user.getUsername())) {
                model.addAttribute("usernameError", "Nazwa użytkownika jest już zajęta");
                return "auth/register";
            }

            if (userService.existsByEmail(user.getEmail())) {
                model.addAttribute("emailError", "Adres email jest już zajęty");
                return "auth/register";
            }

            // Zarejestruj nowego użytkownika
            userService.registerNewUser(user);

            redirectAttributes.addFlashAttribute("message",
                    "Rejestracja przebiegła pomyślnie. Możesz się teraz zalogować.");
            return "redirect:/auth/login";

        } catch (Exception e) {
            model.addAttribute("error", "Wystąpił błąd podczas rejestracji: " + e.getMessage());
            return "auth/register";
        }
    }
}