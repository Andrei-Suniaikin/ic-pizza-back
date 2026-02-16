package com.icpizza.backend.dto.order;

public record OrderInfoTO(
        Long id,
        Integer orderNumber,
        String orderStatus,
        String orderCreated,
        Integer estimationTime
) {
}
