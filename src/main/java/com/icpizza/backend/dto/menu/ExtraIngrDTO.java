package com.icpizza.backend.dto.menu;

import java.math.BigDecimal;

public record ExtraIngrDTO(
        Long id,
        String name,
        String photo,
        Boolean available,
        String size,
        BigDecimal price
) {
}
//
//"id",        ex.getId(),
//                        "name",      ex.getName(),
//                        "photo",     ex.getPhoto(),
//                        "price",     ex.getPrice(),
//                        "size",      nz(ex.getSize()),
//        "available", ex.isAvailable()