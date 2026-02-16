package com.icpizza.backend.dto.order;

import java.math.BigDecimal;

public record OrderPaymentTO(
        String orderId,
        BigDecimal amount,
        String type,
        int branchId
) {
}
