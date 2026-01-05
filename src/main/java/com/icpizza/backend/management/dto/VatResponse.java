package com.icpizza.backend.management.dto;

import java.math.BigDecimal;

public record VatResponse(
        String branchName,
        Long totalOrders,
        BigDecimal totalRevenue
) {
}
