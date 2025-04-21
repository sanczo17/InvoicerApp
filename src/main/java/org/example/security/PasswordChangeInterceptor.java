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

@Component
public class PasswordChangeInterceptor implements HandlerInterceptor {

    private final UserService userService;

    @Autowired
    public PasswordChangeInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            String username = auth.getName();
            Optional<User> userOpt = userService.findByUsername(username);

            if (userOpt.isPresent() && userOpt.get().isMustChangePassword()) {
                String requestURI = request.getRequestURI();
                if (!requestURI.equals("/auth/change-password") &&
                        !requestURI.equals("/auth/perform-change-password") &&
                        !requestURI.equals("/auth/logout") &&
                        !requestURI.startsWith("/css/") &&
                        !requestURI.startsWith("/js/") &&
                        !requestURI.startsWith("/images/")) {

                    response.sendRedirect("/auth/change-password");
                    return false;
                }
            }
        }
        return true;
    }
}