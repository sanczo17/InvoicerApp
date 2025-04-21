package org.example.repository;

import org.example.model.LoginAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repozytorium dla encji LoginAudit.
 */
@Repository
public interface LoginAuditRepository extends JpaRepository<LoginAudit, Long> {

    /**
     * Znajduje wszystkie logi logowania dla danego użytkownika.
     */
    List<LoginAudit> findByUsername(String username);

    /**
     * Znajduje logi logowania stronami dla danego użytkownika.
     */
    Page<LoginAudit> findByUsername(String username, Pageable pageable);

    /**
     * Znajduje logi logowania dla danego użytkownika w określonym przedziale czasowym.
     */
    List<LoginAudit> findByUsernameAndLoginTimeBetween(
            String username, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Znajduje logi logowania po statusie powodzenia.
     */
    List<LoginAudit> findBySuccessful(boolean successful);

    /**
     * Znajduje najnowsze logi logowań.
     */
    List<LoginAudit> findTop10ByOrderByLoginTimeDesc();
}