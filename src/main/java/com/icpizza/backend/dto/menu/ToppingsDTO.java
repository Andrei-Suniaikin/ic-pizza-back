package com.icpizza.backend.dto.menu;

import java.math.BigDecimal;

public record ToppingsDTO(
        Long id,
        String photo,
        String name,
        Boolean available,
        BigDecimal price
) {
}
