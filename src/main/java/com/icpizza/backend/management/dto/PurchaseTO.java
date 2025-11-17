package com.icpizza.backend.management.dto;

import java.math.BigDecimal;
import java.util.List;

public record PurchaseTO(
        Long id,
        String title,
        BigDecimal finalPrice,
        Long userId,
        java.time.LocalDateTime purchaseDate,
        List<PurchaseProductsTO> purchaseProducts
) {
    public record PurchaseProductsTO(
            ProductTO product,
            BigDecimal quantity,
            BigDecimal finalPrice,
            BigDecimal price,
            String vendorName
    ) {
    }
}

