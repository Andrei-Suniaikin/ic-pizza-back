package com.icpizza.backend.dto;

import java.math.BigDecimal;

public record OrderPaymentTO(
        String orderId,
        BigDecimal amount,
        String type,
        int branchId
) {
}
