package com.icpizza.backend.dto;

public record DoughUsageTO(
        String doughType,
        int friday,
        int saturday,
        int sunday,
        int monday,
        int tuesday,
        int wednesday,
        int thursday
) {
}
