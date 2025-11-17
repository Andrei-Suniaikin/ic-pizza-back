package com.icpizza.backend.management.dto;

import java.math.BigDecimal;
import java.util.List;

public record ConsumptionReportTO(
        Long id,
        String title,
        BigDecimal finalPrice,
        Long userId,
        Integer branchNo,
        List<ConsumptionProductTO> consumptionProducts
) {
    public record ConsumptionProductTO(
            String productName,
            BigDecimal quantity,
            BigDecimal finalPrice
    ){}
}
