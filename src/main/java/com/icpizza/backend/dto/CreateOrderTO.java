package com.icpizza.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.icpizza.backend.entity.Order;
import com.icpizza.backend.entity.OrderItem;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;


public record CreateOrderTO (
        @JsonProperty("id")
        Long id,
        @JsonProperty("tel")
        String telephoneNo,
        @JsonProperty("customer_name")
        String customerName,
        @JsonProperty("user_id")
        Long userId,
        @JsonProperty("amount_paid")
        BigDecimal amountPaid,
        @JsonProperty("items")
        List<OrderItemsTO> orderItems,
        @JsonProperty("payment_type")
        String paymentType,
        @JsonProperty("type")
        String orderType,
        String notes,
        String address,
        Boolean isPaid,
        Boolean isReady,
        Integer branchNumber
){
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
                List<ComboItemsTO> comboItems
        ){
                public record ComboItemsTO(
                        String category,
                        String name,
                        String size,
                        Boolean isGarlicCrust,
                        Boolean isThinDough,
                        Integer quantity,
                        String description
                ){}
        }
}
