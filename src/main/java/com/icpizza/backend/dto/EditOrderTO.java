package com.icpizza.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record EditOrderTO(
        @JsonProperty("id")
        Long orderId,

        @JsonProperty("tel")
        String telephoneNo,

        @JsonProperty("amount_paid")
        BigDecimal amountPaid,

        String address,

        @JsonProperty("payment_type")
        String paymentType,

        String notes,

        List<EditOrderItemTO> items,

        String orderNo
) {
    public record EditOrderItemTO(
            String name,
            Integer quantity,
            BigDecimal amount,
            String size,
            String category,

            @JsonProperty("isGarlicCrust")
            Boolean isGarlicCrust,

            @JsonProperty("isThinDough")
            Boolean isThinDough,

            String description,

            @JsonProperty("discount_amount")
            BigDecimal discountAmount
    ) {}
}
