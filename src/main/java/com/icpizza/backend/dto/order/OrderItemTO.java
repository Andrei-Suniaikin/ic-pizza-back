package com.icpizza.backend.dto.order;

public record OrderItemTO(
        String name,
        Integer quantity,
        Double amount,
        String size,
        String category,
        Boolean isGarlicCrust,
        Boolean isThinDough,
        String description,
        java.math.BigDecimal discount_amount,
        String photo
) {
}
