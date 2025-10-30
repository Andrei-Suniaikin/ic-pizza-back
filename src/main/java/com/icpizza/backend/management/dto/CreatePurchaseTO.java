package com.icpizza.backend.management.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreatePurchaseTO(
        String title,
        BigDecimal finalPrice,
        Long userId,
        LocalDate purchaseDate,
        Integer branchNo,
        List<PurchaseProductsTO> purchaseProducts
        ) {
    public record PurchaseProductsTO(
            Long id,
            BigDecimal quantity,
            BigDecimal finalPrice,
            BigDecimal price,
            String vendorName
    ){}
}
