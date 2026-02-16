package com.icpizza.backend.dto.branch;

import com.icpizza.backend.enums.WorkLoadLevel;

import java.util.UUID;

public record UpdateWorkLoadLevelTO(
        WorkLoadLevel level,
        UUID branchId
) {
}
