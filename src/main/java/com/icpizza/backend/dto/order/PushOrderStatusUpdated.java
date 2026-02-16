package com.icpizza.backend.dto.order;

import java.util.UUID;

public record PushOrderStatusUpdated(
        Long id,
        String status,
        UUID branchId
) {
}
