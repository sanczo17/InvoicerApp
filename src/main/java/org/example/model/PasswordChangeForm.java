package org.example.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Klasa formularza do zmiany hasła użytkownika.
 * Przechowuje nowe hasło i jego potwierdzenie.
 */
public class PasswordChangeForm {

    @NotBlank(message = "Nowe hasło jest wymagane")
    @Size(min = 6, message = "Hasło musi mieć co najmniej 6 znaków")
    private String newPassword;

    @NotBlank(message = "Potwierdzenie hasła jest wymagane")
    private String confirmPassword;

    /**
     * Zwraca nowe hasło.
     */
    public String getNewPassword() {
        return newPassword;
    }

    /**
     * Ustawia nowe hasło.
     */
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    /**
     * Zwraca potwierdzenie hasła.
     */
    public String getConfirmPassword() {
        return confirmPassword;
    }

    /**
     * Ustawia potwierdzenie hasła.
     */
    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}