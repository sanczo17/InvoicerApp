package org.example.config;

import org.example.security.InactiveUserInterceptor;
import org.example.security.PasswordChangeInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Konfiguracja webowa aplikacji.
 * Rejestruje interceptory dla żądań HTTP.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final PasswordChangeInterceptor passwordChangeInterceptor;
    private final InactiveUserInterceptor inactiveUserInterceptor;

    @Autowired
    public WebConfig(PasswordChangeInterceptor passwordChangeInterceptor,
                     InactiveUserInterceptor inactiveUserInterceptor) {
        this.passwordChangeInterceptor = passwordChangeInterceptor;
        this.inactiveUserInterceptor = inactiveUserInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Interceptor wymagający zmiany hasła
        registry.addInterceptor(passwordChangeInterceptor);

        // Interceptor blokujący nieaktywnych użytkowników
        registry.addInterceptor(inactiveUserInterceptor);
    }
}