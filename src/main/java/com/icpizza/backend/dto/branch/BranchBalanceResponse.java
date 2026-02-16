package com.icpizza.backend.dto.branch;

import java.math.BigDecimal;

public record BranchBalanceResponse(
        BigDecimal branchBalance
) {
}
