package org.example.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.model.User;
import org.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * Interceptor wymuszający zmianę hasła, jeśli użytkownik ma ustawioną flagę mustChangePassword.
 * Przekierowuje użytkownika do strony zmiany hasła, gdy próbuje uzyskać dostęp do innych stron.
 */
@Component
public class PasswordChangeInterceptor implements HandlerInterceptor {

    private final UserService userService;

    @Autowired
    public PasswordChangeInterceptor(UserService userService) {
        this.userService = userService;
    }

    /**
     * Sprawdza, czy użytkownik musi zmienić hasło i przekierowuje do odpowiedniej strony.
     * Uruchamiane przed obsługą żądania przez kontroler.
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Pobierz aktualną autentykację
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Kontynuuj tylko gdy użytkownik jest zalogowany
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            String username = auth.getName();

            try {
                // Pobierz użytkownika z bazy (metoda findByUsername może rzucić wyjątek)
                Optional<User> userOpt = userService.findByUsername(username);
                User user = userOpt.orElse(null);

                // Jeśli użytkownik musi zmienić hasło
                if (user != null && user.isMustChangePassword()) {
                    String requestURI = request.getRequestURI();

                    // Pozwól na dostęp tylko do stron związanych ze zmianą hasła i wylogowaniem
                    if (!requestURI.equals("/auth/change-password") &&
                            !requestURI.equals("/auth/perform-change-password") &&
                            !requestURI.equals("/auth/logout") &&
                            !requestURI.startsWith("/css/") &&
                            !requestURI.startsWith("/js/") &&
                            !requestURI.startsWith("/images/")) {

                        // Przekieruj do strony zmiany hasła
                        response.sendRedirect("/auth/change-password");
                        return false;
                    }
                }
            } catch (Exception e) {
                // Jeśli nie można znaleźć użytkownika, pozwól na kontynuowanie
                // Obsługa błędów może być rozszerzona w przyszłości
            }
        }

        // Kontynuuj normalne przetwarzanie żądania
        return true;
    }
}