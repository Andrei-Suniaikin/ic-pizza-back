package com.icpizza.backend.dto.branch;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.icpizza.backend.enums.EventType;

import java.math.BigDecimal;
import java.util.UUID;

public record ShiftEventRequest(
        @JsonProperty("branch_id") UUID branchId,
        @JsonProperty("type") EventType type,
        @JsonProperty("prep_plan") String prepPlan,
        @JsonProperty("cash_amount") BigDecimal cashAmount
) {
}


