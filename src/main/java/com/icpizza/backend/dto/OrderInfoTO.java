package com.icpizza.backend.dto;

public record OrderInfoTO(
        Long id,
        Integer orderNumber,
        String orderStatus,
        String orderCreated,
        Integer estimationTime
) {
}
