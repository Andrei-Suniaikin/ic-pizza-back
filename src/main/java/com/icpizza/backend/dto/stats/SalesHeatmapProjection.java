package com.icpizza.backend.dto.stats;

import java.math.BigDecimal;

public interface SalesHeatmapProjection {
        String getDayName();
        Integer getHourOfDay();
        BigDecimal getTotalSales();
}
