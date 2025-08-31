package com.icpizza.backend.dto;

import java.util.List;

public record UpdateAvailabilityRequest(
        List<Change> changes
) {
    public record Change(
            String type,
            String name,
            Boolean enabled
    ){}
}
