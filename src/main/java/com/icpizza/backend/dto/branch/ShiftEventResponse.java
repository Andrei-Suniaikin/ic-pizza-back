package com.icpizza.backend.dto.branch;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.UUID;

public record ShiftEventResponse(
        String status,
        UUID id,
        @JsonProperty("shiftNo") Integer shiftNo,
        CashWarning cashWarning
){
    public record CashWarning(String error, BigDecimal expected) {}

}
