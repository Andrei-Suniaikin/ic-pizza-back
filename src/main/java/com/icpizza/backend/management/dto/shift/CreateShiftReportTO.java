package com.icpizza.backend.management.dto.shift;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CreateShiftReportTO(
        String title,
        Double totalHours,
        Integer branchNo,
        List<ShiftInfoTO> shifts
) {
    public record ShiftInfoTO(
            LocalDate shiftDate,
            LocalTime startTime,
            LocalTime endTime,
            Double total
    ) {
    }
}
