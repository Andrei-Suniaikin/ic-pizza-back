package com.icpizza.backend.management.dto.shift;

public record BaseShiftResponse(
        Long id,
        String title,
        Integer branchNo,
        Double cookTotalHours,
        Double managerTotalHours
) {
}
