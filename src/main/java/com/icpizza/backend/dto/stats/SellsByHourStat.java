package com.icpizza.backend.dto.stats;

import java.math.BigDecimal;
import java.util.Map;

public record SellsByHourStat(
        int Hour,
        Map<String, BigDecimal> sellsByDay
) {
}
