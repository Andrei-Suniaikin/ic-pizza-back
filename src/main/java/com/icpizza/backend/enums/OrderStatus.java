package com.icpizza.backend.enums;

public enum OrderStatus {
    PENDING("Pending"),
    KITCHEN_PHASE("Kitchen Phase"),
    READY("Ready"),
    ACCEPTED("Accepted"),
    REJECTED("Rejected"),
    A("Accepted"),
    N("New"),
    O("Out for delivery"),
    D("Delivered"),
    C("Cancelled"),
    R("Rejected"),
    T("Timed out");

    private final String label;

    OrderStatus(String label) { this.label = label; }

    /** Англоязычный лейбл “как на бейдже” */
    public String label() { return label; }

    /** Безопасный доступ (null → "—") */
    public static String toLabel(OrderStatus s) {
        return s == null ? "—" : s.label();
    }
}
