package org.example.model.enums;

import jakarta.persistence.Converter;
import jakarta.persistence.AttributeConverter;

/**
 * Enum reprezentujący metody płatności za fakturę.
 * Zawiera również konwerter do mapowania między bazą danych a Java.
 */
public enum PaymentMethod {
    PRZELEW("Przelew"),
    GOTOWKA("Gotówka"),
    KARTA("Karta");

    private final String displayName;

    /**
     * Tworzy metodę płatności z określoną nazwą wyświetlaną.
     *
     * @param displayName nazwa wyświetlana (może zawierać polskie znaki)
     */
    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Zwraca nazwę wyświetlaną metody płatności.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Znajduje metodę płatności po nazwie wyświetlanej.
     *
     * @param displayName nazwa wyświetlana do znalezienia
     * @return odpowiadająca metoda płatności
     * @throws IllegalArgumentException gdy nie znaleziono metody płatności
     */
    public static PaymentMethod fromDisplayName(String displayName) {
        for (PaymentMethod method : values()) {
            if (method.getDisplayName().equalsIgnoreCase(displayName)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Nieznana metoda płatności: " + displayName);
    }

    /**
     * Zwraca nazwę wyświetlaną metody płatności jako String.
     */
    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Konwerter do obsługi mapowania między bazą danych a enumem.
     * Pozwala na przechowywanie nazw bez polskich znaków w bazie danych,
     * jednocześnie prezentując użytkownikowi nazwy z polskimi znakami.
     */
    @Converter(autoApply = true)
    public static class PaymentMethodConverter implements AttributeConverter<PaymentMethod, String> {

        /**
         * Konwertuje enum na String zapisywany w bazie danych.
         */
        @Override
        public String convertToDatabaseColumn(PaymentMethod method) {
            return method != null ? method.name() : null;
        }

        /**
         * Konwertuje String z bazy danych na enum.
         * Obsługuje również przypadki szczególne.
         */
        @Override
        public PaymentMethod convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.isBlank()) {
                return PaymentMethod.PRZELEW;
            }

            try {
                return PaymentMethod.valueOf(dbData);
            } catch (IllegalArgumentException e) {
                for (PaymentMethod method : PaymentMethod.values()) {
                    if (method.getDisplayName().equalsIgnoreCase(dbData)) {
                        return method;
                    }
                }
                return PaymentMethod.PRZELEW;
            }
        }
    }
}