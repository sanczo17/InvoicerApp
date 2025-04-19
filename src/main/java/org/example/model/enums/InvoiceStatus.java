package org.example.model.enums;

public enum InvoiceStatus {
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
}