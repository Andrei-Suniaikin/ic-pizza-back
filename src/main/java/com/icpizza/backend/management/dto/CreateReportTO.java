package com.icpizza.backend.management.dto;

import com.icpizza.backend.management.enums.ReportType;

import java.math.BigDecimal;
import java.util.List;

public record CreateReportTO(
        String title,
        ReportType type,
        Integer branchNo,
        Long userId,
        List<CreateReportProductsTO> inventoryProducts
) {
    public record CreateReportProductsTO(
            Long id,
            BigDecimal quantity,
            BigDecimal finalPrice
    ){}
}
