package com.icpizza.backend.management.dto.shift;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ShiftReportTO(
        Long id,
        String title,
        Double cookTotalHours,
        Double managerTotalHours,
        LocalDate creationTimeStamp,
        Integer branchNo,
        List<ShiftInfoTO> shifts
) {
    public record ShiftInfoTO(
            LocalDate shiftDate,
            LocalTime cookStartTime,
            LocalTime cookEndTime,
            Double cookTotal,
            LocalTime managerStartTime,
            LocalTime managerEndTime,
            Double managerTotal
    ){}
}
