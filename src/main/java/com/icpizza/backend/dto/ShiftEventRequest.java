package com.icpizza.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.icpizza.backend.enums.EventType;

import java.math.BigDecimal;

public record ShiftEventRequest(
        @JsonProperty("branch_id") String branchId,
        @JsonProperty("type") EventType type,
        @JsonProperty("prep_plan") String prepPlan,
        @JsonProperty("cash_amount") BigDecimal cashAmount
) {
}


