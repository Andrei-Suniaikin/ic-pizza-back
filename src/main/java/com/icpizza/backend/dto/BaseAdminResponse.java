package com.icpizza.backend.dto;

import com.icpizza.backend.enums.EventType;
import com.icpizza.backend.enums.WorkLoadLevel;

import java.util.UUID;

public record BaseAdminResponse(
        WorkLoadLevel level,
        EventType cashStage,
        EventType checklistStage,
        UUID branchId
) {
}
