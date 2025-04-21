package org.example.service;

import org.example.model.LoginAudit;
import org.example.repository.LoginAuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Serwis do zarządzania audytem logowań.
 */
@Service
public class LoginAuditService {

    private static final Logger logger = LoggerFactory.getLogger(LoginAuditService.class);

    private final LoginAuditRepository loginAuditRepository;

    @Autowired
    public LoginAuditService(LoginAuditRepository loginAuditRepository) {
        this.loginAuditRepository = loginAuditRepository;
    }

    /**
     * Zapisuje informację o próbie logowania.
     */
    public LoginAudit saveLoginAttempt(String username, String ipAddress,
                                       String userAgent, boolean successful) {
        LoginAudit audit = new LoginAudit(username, ipAddress, userAgent, successful);

        if (successful) {
            logger.info("Udane logowanie użytkownika: {}, IP: {}", username, ipAddress);
        } else {
            logger.warn("Nieudane logowanie dla użytkownika: {}, IP: {}", username, ipAddress);
        }

        return loginAuditRepository.save(audit);
    }

    /**
     * Pobiera historię logowań dla użytkownika.
     */
    public List<LoginAudit> getLoginHistoryForUser(String username) {
        return loginAuditRepository.findByUsername(username);
    }

    /**
     * Pobiera stronicowaną historię logowań dla użytkownika.
     */
    public Page<LoginAudit> getLoginHistoryForUser(String username, Pageable pageable) {
        return loginAuditRepository.findByUsername(username, pageable);
    }

    /**
     * Pobiera ostatnie próby logowań.
     */
    public List<LoginAudit> getRecentLoginAttempts() {
        return loginAuditRepository.findTop10ByOrderByLoginTimeDesc();
    }

    /**
     * Pobiera nieudane próby logowań.
     */
    public List<LoginAudit> getFailedLoginAttempts() {
        return loginAuditRepository.findBySuccessful(false);
    }

    /**
     * Pobiera historię logowań dla użytkownika w określonym przedziale czasowym.
     */
    public List<LoginAudit> getLoginHistoryForUserInPeriod(
            String username, LocalDateTime startDate, LocalDateTime endDate) {
        return loginAuditRepository.findByUsernameAndLoginTimeBetween(username, startDate, endDate);
    }
}