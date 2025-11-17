package com.icpizza.backend.management.dto;

import java.math.BigDecimal;

public record ProductInfo(
        Long id,
        BigDecimal quantity,
        BigDecimal finalPrice
) {
    public static final ProductInfo ZERO = new ProductInfo(null, BigDecimal.ZERO, BigDecimal.ZERO);
}
