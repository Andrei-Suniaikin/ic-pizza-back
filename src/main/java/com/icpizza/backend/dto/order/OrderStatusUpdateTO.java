package com.icpizza.backend.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record OrderStatusUpdateTO(
        @JsonProperty("orderId")
        Long orderId,
        @JsonProperty("jahezOrderId")
        Long jahezOrderId,
        @JsonProperty("orderStatus")
        String orderStatus,
        String reason,
        UUID branchId
) {
}
