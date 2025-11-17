package com.icpizza.backend.management.dto;

import java.math.BigDecimal;

public record BasePurchaseResponse(
        Long id,
        String title,
        BigDecimal finalPrice,
        java.time.LocalDateTime createdAt
) {
}
