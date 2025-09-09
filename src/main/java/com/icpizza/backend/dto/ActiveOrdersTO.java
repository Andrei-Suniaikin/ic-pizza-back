package com.icpizza.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.sql.Time;

public record ActiveOrdersTO(
        @JsonProperty("id")
        Long id,
        Integer order_no,
        String order_type,
        java.math.BigDecimal amount_paid,
        String phone_number,
        String address,
        java.math.BigDecimal sale_amount,
        String customer_name,
        String order_created,
        String payment_type,
        String notes,
        String status,
        Boolean isReady,
        Boolean isPaid,
        Boolean isPickedUp,
        java.util.List<ActiveOrderItemTO> items
) {
    public record ActiveOrderItemTO(
            String name,
            Integer quantity,
            java.math.BigDecimal amount,
            String size,
            String category,
            boolean isGarlicCrust,
            boolean isThinDough,
            String description,
            java.math.BigDecimal discountAmount,
            String photo
    ) {}
}
