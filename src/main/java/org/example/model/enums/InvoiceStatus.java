package org.example.model.enums;

import jakarta.persistence.Converter;
import jakarta.persistence.AttributeConverter;

public enum InvoiceStatus {
    // Używamy wartości bez polskich znaków jako identyfikatory w enum, aby uniknąć problemów z kodowaniem
    OPLACONA("OPŁACONA"),
    NIEOPLACONA("NIEOPŁACONA");

    private final String displayName;

    InvoiceStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    // Konwerter do obsługi mapowania między bazą danych a enumem
    @Converter(autoApply = true)
    public static class InvoiceStatusConverter implements AttributeConverter<InvoiceStatus, String> {

        @Override
        public String convertToDatabaseColumn(InvoiceStatus status) {
            return status != null ? status.name() : null;
        }

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