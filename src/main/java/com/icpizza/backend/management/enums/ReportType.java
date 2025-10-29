package com.icpizza.backend.management.enums;

public enum ReportType {
    INVENTORY("Inventory"),
    PURCHASE("Purchase");

    private final String label;
    ReportType(String label) {
        this.label = label;
    }
}
