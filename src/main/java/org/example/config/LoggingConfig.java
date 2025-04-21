package org.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Konfiguracja dla systemu logowania.
 * Zapewnia, że katalog logów istnieje przed uruchomieniem aplikacji.
 */
@Configuration
public class LoggingConfig {

    private static final Logger logger = LoggerFactory.getLogger(LoggingConfig.class);

    @Value("${logging.directory:logs}")
    private String logsDirectory;

    /**
     * Tworzy katalog logów, jeśli nie istnieje.
     */
    @PostConstruct
    public void init() {
        try {
            File logsDir = new File(logsDirectory);
            if (!logsDir.exists()) {
                Files.createDirectories(Paths.get(logsDirectory));
                Files.createDirectories(Paths.get(logsDirectory + "/archived"));
                logger.info("Utworzono katalog logów: {}", logsDir.getAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("Nie można utworzyć katalogu logów: {}", e.getMessage(), e);
        }
    }
}