package org.example.model.enums;

/**
 * Metoda płatności
 */
public enum PaymentMethod {
    PRZELEW("Przelew"),
    GOTOWKA("Gotówka"),
    KARTA("Karta");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}