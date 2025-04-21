package org.example.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.model.User;
import org.example.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * Interceptor sprawdzający czy konto użytkownika jest aktywne.
 * Blokuje dostęp do aplikacji dla użytkowników z nieaktywnymi kontami.
 */
@Component
public class InactiveUserInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(InactiveUserInterceptor.class);

    private final UserService userService;

    @Autowired
    public InactiveUserInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Kontynuuj tylko gdy użytkownik jest zalogowany
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            String username = auth.getName();

            try {
                Optional<User> userOpt = userService.findByUsername(username);

                if (userOpt.isPresent() && !userOpt.get().isActive()) {
                    logger.warn("Próba dostępu przez nieaktywnego użytkownika: {}", username);

                    // Wyloguj użytkownika
                    SecurityContextHolder.clearContext();

                    // Przekieruj do strony logowania z informacją
                    response.sendRedirect("/auth/login?inactive=true");
                    return false;
                }
            } catch (Exception e) {
                logger.error("Błąd podczas sprawdzania statusu konta użytkownika: {}", e.getMessage());
            }
        }

        return true;
    }
}