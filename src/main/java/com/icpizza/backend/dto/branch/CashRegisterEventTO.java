package com.icpizza.backend.dto.branch;

import com.icpizza.backend.enums.EventType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CashRegisterEventTO(
        UUID id,
        String notes,
        UUID branchId,
        BigDecimal amount,
        EventType type,
        LocalDateTime date
) {
}
