package org.example.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.service.LoginAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handler wywoływany po pomyślnym uwierzytelnieniu.
 * Zapisuje informacje o udanym logowaniu.
 */
@Component
public class LoginAuditAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final LoginAuditService loginAuditService;

    @Autowired
    public LoginAuditAuthenticationSuccessHandler(LoginAuditService loginAuditService) {
        this.loginAuditService = loginAuditService;
        setDefaultTargetUrl("/");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws ServletException, IOException {

        // Zapisz informację o udanym logowaniu
        String username = authentication.getName();
        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        loginAuditService.saveLoginAttempt(username, ipAddress, userAgent, true);

        // Kontynuuj standardową obsługę
        super.onAuthenticationSuccess(request, response, authentication);
    }
}