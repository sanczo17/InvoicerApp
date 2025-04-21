package org.example.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PasswordChangeForm {

    @NotBlank(message = "Nowe hasło jest wymagane")
    @Size(min = 6, message = "Hasło musi mieć co najmniej 6 znaków")
    private String newPassword;

    @NotBlank(message = "Potwierdzenie hasła jest wymagane")
    private String confirmPassword;

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}