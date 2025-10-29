package com.icpizza.backend.management.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BasePurchaseResponse(
        Long id,
        String title,
        BigDecimal finalPrice,
        LocalDate createdAt
) {
}
