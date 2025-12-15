package com.icpizza.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrderResponse(
        @JsonProperty("id")
        Long id,
        @JsonProperty("tel")
        String telephoneNo,
        @JsonProperty("customer_name")
        String customerName,
        @JsonProperty("amount_paid")
        BigDecimal amountPaid,
        @JsonProperty("items")
        List<CreateOrderTO.OrderItemsTO> orderItems,
        @JsonProperty("payment_type")
        String paymentType,
        @JsonProperty("type")
        String orderType,
        String notes,
        String address,
        Boolean isPaid,
        Integer branchNumber,
        Boolean isNewCustomer
) {
    public record OrderItemsTO(
            BigDecimal amount,
            String category,
            String description,
            @JsonProperty("discount_amount")
            BigDecimal discountAmount,
            boolean isGarlicCrust,
            boolean isThinDough,
            String name,
            Integer quantity,
            String size,
            List<CreateOrderTO.OrderItemsTO.ComboItemsTO> comboItems
    ) {
        public record ComboItemsTO(
                String category,
                String name,
                String size,
                Boolean isGarlicCrust,
                Boolean isThinDough,
                Integer quantity,
                String description
        ) {
        }
    }
}
