package org.example.model.enums;

import jakarta.persistence.Converter;
import jakarta.persistence.AttributeConverter;

/**
 * Enum reprezentujący status faktury (opłacona/nieopłacona).
 * Zawiera również konwerter do mapowania między bazą danych a Java.
 */
public enum InvoiceStatus {
    // Używamy wartości bez polskich znaków jako identyfikatory w enum, aby uniknąć problemów z kodowaniem
    OPLACONA("OPŁACONA"),
    NIEOPLACONA("NIEOPŁACONA");

    private final String displayName;

    /**
     * Tworzy status faktury z określoną nazwą wyświetlaną.
     *
     * @param displayName nazwa wyświetlana (może zawierać polskie znaki)
     */
    InvoiceStatus(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Zwraca nazwę wyświetlaną statusu.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Zwraca nazwę wyświetlaną statusu jako String.
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
    public static class InvoiceStatusConverter implements AttributeConverter<InvoiceStatus, String> {

        /**
         * Konwertuje enum na String zapisywany w bazie danych.
         */
        @Override
        public String convertToDatabaseColumn(InvoiceStatus status) {
            return status != null ? status.name() : null;
        }

        /**
         * Konwertuje String z bazy danych na enum.
         * Obsługuje również starsze dane i przypadki szczególne.
         */
        @Override
        public InvoiceStatus convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.isEmpty() || dbData.isBlank()) {
                // Domyślna wartość dla pustych lub null w bazie danych
                return InvoiceStatus.NIEOPLACONA;
            }

            try {
                return InvoiceStatus.valueOf(dbData);
            } catch (IllegalArgumentException e) {
                // Próbujemy dopasować po displayName dla starych danych
                for (InvoiceStatus status : InvoiceStatus.values()) {
                    if (status.getDisplayName().equals(dbData)) {
                        return status;
                    }
                }

                // Dodatkowe przypadki dla innych możliwych wartości w bazie
                if (dbData.contains("OPŁAC") || dbData.contains("OPLAC")) {
                    return InvoiceStatus.OPLACONA;
                } else if (dbData.contains("NIEOPŁAC") || dbData.contains("NIEOPLAC")) {
                    return InvoiceStatus.NIEOPLACONA;
                }

                // Jeśli nic nie pasuje, zwracamy domyślną wartość
                return InvoiceStatus.NIEOPLACONA;
            }
        }
    }
}