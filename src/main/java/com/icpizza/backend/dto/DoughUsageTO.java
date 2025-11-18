package com.icpizza.backend.dto;

import java.time.LocalDate;
import java.util.List;

public record DoughUsageTO(
        String doughType,
        List<DoughDailyUsageTO> history
) {
    public record DoughDailyUsageTO(
            LocalDate date,
            int quantity
    ) {}
}
