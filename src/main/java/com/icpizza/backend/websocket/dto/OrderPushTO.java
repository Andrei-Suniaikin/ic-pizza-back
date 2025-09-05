package com.icpizza.backend.websocket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record OrderPushTO(
        @JsonProperty("id") Long orderId,
        @JsonProperty("order_no") Integer orderNo,
        @JsonProperty("order_type") String orderType,
        @JsonProperty("amount_paid") BigDecimal amountPaid,
        @JsonProperty("phone_number") String phoneNumber,
        @JsonProperty("address") String address,
        @JsonProperty("sale_amount") BigDecimal saleAmount,
        @JsonProperty("customer_name") String customerName,
        @JsonProperty("order_created") String orderCreated,
        @JsonProperty("payment_type") String paymentType,
        @JsonProperty("notes") String notes,
        @JsonProperty("external_id") Long externalId, // ← сюда кладём Jahez id (или null)
        @JsonProperty("items") List<ItemTO> items,
        Boolean isReady,
        Boolean isPaid
) {
    public record ItemTO(
            String name,
            Integer quantity,
            BigDecimal amount,
            String size,
            String category,
            String description,
            @JsonProperty("isGarlicCrust") boolean isGarlicCrust,
            @JsonProperty("isThinDough") boolean isThinDough,
            @JsonProperty("discount_amount") BigDecimal discountAmount,
            String photo
    ) {}
}
