package com.icpizza.backend.management.dto;

import jakarta.persistence.Column;

import java.math.BigDecimal;

public record ProductTO(
         Long id,
         String name,
         BigDecimal price,
         Boolean isInventory
) {
}
