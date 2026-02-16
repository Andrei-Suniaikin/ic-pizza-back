package com.icpizza.backend.dto.menu;

import java.util.List;
import java.util.UUID;

public record UpdateAvailabilityRequest(
        List<Change> changes,
        UUID branchId
) {
    public record Change(
            String type,
            String name,
            Boolean enabled
    ){}
}
