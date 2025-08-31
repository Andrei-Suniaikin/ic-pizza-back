package com.icpizza.backend.dto;

public record OrderStatusUpdateTO(
        Long orderId,
        Long jahezOrderId,
        String orderStatus,
        String reason
) {
}
