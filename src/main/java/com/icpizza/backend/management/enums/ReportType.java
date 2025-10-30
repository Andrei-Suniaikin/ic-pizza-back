package com.icpizza.backend.management.enums;

public enum ReportType {
    INVENTORY("Inventory"),
    PURCHASE("Purchase"),
    PRODUCT_CONSUMPTION("Product Consumption"),;

    public final String label;
    ReportType(String label) {
        this.label = label;
    }
}
