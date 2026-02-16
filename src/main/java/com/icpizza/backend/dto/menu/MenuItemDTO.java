package com.icpizza.backend.dto.menu;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record MenuItemDTO(
        Long id,
        String name,
        String category,
        String size,
        String photo,
        String description,
        @JsonProperty("available")
        Boolean isAvailable,
        @JsonProperty("is_best_seller")
        Boolean isBestSeller,
        BigDecimal price
) {
}
