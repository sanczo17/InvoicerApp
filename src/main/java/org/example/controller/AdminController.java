package org.example.controller;

import org.example.model.User;
import org.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    @Autowired
    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String adminPanel(Model model) {
        List<User> users = userService.findAll();

        long activeUsers = users.stream().filter(User::isActive).count();
        long inactiveUsers = users.size() - activeUsers;

        model.addAttribute("totalUsers", users.size());
        model.addAttribute("activeUsers", activeUsers);
        model.addAttribute("inactiveUsers", inactiveUsers);

        return "admin/dashboard";
    }

    @GetMapping("/roles/grant-admin")
    public String grantAdminRole(Long userId, RedirectAttributes redirectAttributes) {
        try {
            userService.grantAdminRole(userId);
            redirectAttributes.addFlashAttribute("message", "Przyznano uprawnienia administratora");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Wystąpił błąd: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/roles/revoke-admin")
    public String revokeAdminRole(Long userId, RedirectAttributes redirectAttributes) {
        try {
            userService.revokeAdminRole(userId);
            redirectAttributes.addFlashAttribute("message", "Odebrano uprawnienia administratora");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Wystąpił błąd: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
}