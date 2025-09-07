package com.icpizza.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record ShiftEventResponse(
        String status,
        String id,
        @JsonProperty("shiftNo") Integer shiftNo,
        CashWarning cashWarning
){
    public record CashWarning(String error, BigDecimal expected) {}

}
