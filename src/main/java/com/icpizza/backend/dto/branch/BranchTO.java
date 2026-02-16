package com.icpizza.backend.dto.branch;

import java.math.BigDecimal;

public record BranchTO(
        java.util.UUID id,
        String externalId,
        Integer branchNo,
        String branchName,
        BigDecimal branchBalance
) {
}
