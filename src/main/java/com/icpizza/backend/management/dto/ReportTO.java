package com.icpizza.backend.management.dto;

import com.icpizza.backend.management.entity.Product;
import com.icpizza.backend.management.enums.ReportType;

import java.math.BigDecimal;
import java.util.List;

public record ReportTO(
        Long id,
        String title,
        ReportType type,
        Integer branchNo,
        Long userId,
        List<ReportTO.InventoryProductsTO> inventoryProducts
) {
    public record InventoryProductsTO(
            ProductTO product,
            BigDecimal quantity,
            BigDecimal finalPrice
    ){}
}
