package com.icpizza.backend.dto;

import com.icpizza.backend.enums.WorkLoadLevel;

public record UpdateWorkLoadLevelTO(
        WorkLoadLevel level,
        Integer branchNumber
) {
}
