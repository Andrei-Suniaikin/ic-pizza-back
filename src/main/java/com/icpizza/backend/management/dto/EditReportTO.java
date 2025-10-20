package com.icpizza.backend.management.dto;

import com.icpizza.backend.management.entity.Product;
import com.icpizza.backend.management.enums.ReportType;

import java.math.BigDecimal;
import java.util.List;

public record EditReportTO(
        Long id,
        String title,
        ReportType type,
        Integer branchNo,
        Long userId,
        List<EditReportTO.EditReportProductsTO> inventoryProducts
) {
    public record EditReportProductsTO(
            Long id,
            BigDecimal quantity,
            BigDecimal finalPrice
    ){}
}
