package com.icpizza.backend.dto;

import java.util.UUID;

public record PushOrderStatusUpdated(
        Long id,
        String status,
        UUID branchId
) {
}
