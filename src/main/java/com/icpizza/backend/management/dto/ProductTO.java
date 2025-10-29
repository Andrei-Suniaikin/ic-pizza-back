package com.icpizza.backend.management.dto;

import java.math.BigDecimal;

public record ProductTO(
         Long id,
         String name,
         BigDecimal price,
         BigDecimal targetPrice,
         Boolean isInventory,
         Boolean isBundle,
         Boolean isPurchasable,
         String topVendor
) {
}
