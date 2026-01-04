package com.icpizza.backend.dto;

import com.icpizza.backend.enums.WorkLoadLevel;

import java.util.UUID;

public record UpdateWorkLoadLevelTO(
        WorkLoadLevel level,
        UUID branchId
) {
}
