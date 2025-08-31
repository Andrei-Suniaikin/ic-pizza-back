package com.icpizza.backend.dto;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;


public record OrderHistoryTO(
        Long id,
        @JsonProperty("order_no") Integer orderNo,
        @JsonProperty("order_type") String orderType,
        @JsonProperty("amount_paid") BigDecimal amountPaid,
        @JsonProperty("phone_number") String phoneNumber,
        @JsonProperty("sale_amount") BigDecimal saleAmount,
        @JsonProperty("customer_name") String customerName,
        @JsonProperty("order_created") String orderCreated,
        @JsonProperty("payment_type") String paymentType,
        String notes,
        List<OrderItemHistoryTO> items
){
    public record OrderItemHistoryTO(
            String name,
            Integer quantity,
            BigDecimal amount,
            String size,
            String category,
            @JsonProperty("isGarlicCrust") boolean isGarlicCrust,
            @JsonProperty("isThinDough") boolean isThinDough,
            String description,
            @JsonProperty("discount_amount") BigDecimal discountAmount,
            String photo
    ) {}
}
