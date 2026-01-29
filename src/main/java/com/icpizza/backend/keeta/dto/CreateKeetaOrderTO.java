package com.icpizza.backend.keeta.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateKeetaOrderTO(
        String orderId,
        @JsonProperty("orderItems")
        List<OrderItemTO> items,
        String telephoneNo,
        String address,
        String name,
        String description,
        String paymentType,
        BigDecimal amountPaid,
        @JsonProperty("timestamp")
        LocalDateTime createdAt,
        Integer status,
        UUID branchId
) {
    public record OrderItemTO(
            String name,
            String size,
            Integer quantity,
            BigDecimal price,
            String category,
            @JsonProperty("is_garlic_crust")
            Boolean isGarlicCrust,
            @JsonProperty("is_thin_dough")
            Boolean isThinDough,
            String description,
            @JsonProperty("combo_items")
            List<ComboItemTO> comboItemTOList
    ) {
        public record ComboItemTO(
                String name,
                Integer quantity,
                String category,
                @JsonProperty("is_garlic_crust")
                Boolean isGarlicCrust,
                @JsonProperty("is_thin_dough")
                Boolean isThinDough,
                String size,
                String description
                ) {}
    }
}