package org.example.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.service.LoginAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handler wywoływany po nieudanym uwierzytelnieniu.
 * Zapisuje informacje o nieudanym logowaniu.
 */
@Component
public class LoginAuditAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final LoginAuditService loginAuditService;

    @Autowired
    public LoginAuditAuthenticationFailureHandler(LoginAuditService loginAuditService) {
        this.loginAuditService = loginAuditService;
        setDefaultFailureUrl("/auth/login?error=true");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        // Zapisz informację o nieudanym logowaniu
        String username = request.getParameter("username");
        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        loginAuditService.saveLoginAttempt(username, ipAddress, userAgent, false);

        // Kontynuuj standardową obsługę
        super.onAuthenticationFailure(request, response, exception);
    }
}