package com.icpizza.backend.dto;

import com.icpizza.backend.enums.EventType;
import com.icpizza.backend.enums.WorkLoadLevel;

public record BaseAdminResponse(
        WorkLoadLevel level,
        EventType cashStage,
        EventType checklistStage,
        Integer branchNumber
) {
}
