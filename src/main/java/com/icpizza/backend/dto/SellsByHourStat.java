package com.icpizza.backend.dto;

import java.math.BigDecimal;
import java.util.Map;

public record SellsByHourStat(
        int Hour,
        Map<String, BigDecimal> sellsByDay
) {
}
