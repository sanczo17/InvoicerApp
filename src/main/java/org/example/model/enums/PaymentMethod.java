package org.example.model.enums;

import jakarta.persistence.Converter;
import jakarta.persistence.AttributeConverter;

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

    public static PaymentMethod fromDisplayName(String displayName) {
        for (PaymentMethod method : values()) {
            if (method.getDisplayName().equalsIgnoreCase(displayName)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Nieznana metoda płatności: " + displayName);
    }

    @Override
    public String toString() {
        return displayName;
    }

    @Converter(autoApply = true)
    public static class PaymentMethodConverter implements AttributeConverter<PaymentMethod, String> {

        @Override
        public String convertToDatabaseColumn(PaymentMethod method) {
            return method != null ? method.name() : null;
        }

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