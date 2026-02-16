package com.icpizza.backend.dto.branch;

import com.icpizza.backend.enums.CashUpdateType;

import java.math.BigDecimal;
import java.util.UUID;

public record CashUpdateRequest(
        UUID branchId,
        CashUpdateType cashUpdateType,
        BigDecimal amount,
        String note
) {
}
