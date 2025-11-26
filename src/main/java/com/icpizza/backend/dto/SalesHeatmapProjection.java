package com.icpizza.backend.dto;

import java.math.BigDecimal;

public interface SalesHeatmapProjection {
        String getDayName();
        Integer getHourOfDay();
        BigDecimal getTotalSales();
}
