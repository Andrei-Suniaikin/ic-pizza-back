package com.icpizza.backend.management.dto.shift;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record EditShiftReportTO(
        Long id,
        String title,
        Integer totalHours,
        LocalDate creationTimeStamp,
        Integer branchNo,
        List<EditShiftInfoTO> shifts
) {
    public record EditShiftInfoTO(
            LocalDate shiftDate,
            LocalTime cookStartTime,
            LocalTime cookEndTime,
            Double cookTotal,
            LocalTime managerStartTime,
            LocalTime managerEndTime,
            Double managerTotal
    ) {}
}
