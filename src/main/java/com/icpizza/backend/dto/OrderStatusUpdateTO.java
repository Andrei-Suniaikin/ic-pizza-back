package com.icpizza.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OrderStatusUpdateTO(
        @JsonProperty("orderId")
        Long orderId,
        @JsonProperty("jahezOrderId")
        Long jahezOrderId,
        @JsonProperty("orderStatus")
        String orderStatus,
        String reason
) {
}
