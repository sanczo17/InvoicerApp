package org.example.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.NoSuchElementException;

/**
 * Globalny handler wyjątków dla całej aplikacji.
 * Przechwytuje wyjątki rzucane przez kontrolery i serwisy,
 * zapewniając spójną obsługę błędów w całej aplikacji.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Obsługuje wyjątki typu ResourceNotFoundException.
     * Zwraca status 404 Not Found i przekierowuje do strony błędu.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleResourceNotFoundException(ResourceNotFoundException ex, Model model) {
        logger.error("Nie znaleziono zasobu: {}", ex.getMessage());
        model.addAttribute("error", ex.getMessage());
        return "error/404";
    }

    /**
     * Obsługuje wyjątki typu IllegalArgumentException.
     * Zwraca status 400 Bad Request i przekierowuje na poprzednią stronę z komunikatem błędu.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleIllegalArgumentException(IllegalArgumentException ex,
                                                       RedirectAttributes redirectAttributes) {
        logger.error("Nieprawidłowe dane wejściowe: {}", ex.getMessage());

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("error", ex.getMessage());
        modelAndView.setViewName("error/400");
        return modelAndView;
    }

    /**
     * Obsługuje wyjątki typu NoSuchElementException.
     * Używany gdy nie można znaleźć elementu w bazie danych.
     */
    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoSuchElementException(NoSuchElementException ex, Model model) {
        logger.error("Element nie istnieje: {}", ex.getMessage());
        model.addAttribute("error", "Żądany element nie został znaleziony");
        return "error/404";
    }

    /**
     * Obsługuje wszystkie inne wyjątki, które nie zostały obsłużone wcześniej.
     * Zwraca status 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGenericException(Exception ex, Model model) {
        logger.error("Wystąpił nieoczekiwany błąd: {}", ex.getMessage(), ex);
        model.addAttribute("error", "Wystąpił nieoczekiwany błąd. Prosimy spróbować ponownie później.");
        return "error/500";
    }
}