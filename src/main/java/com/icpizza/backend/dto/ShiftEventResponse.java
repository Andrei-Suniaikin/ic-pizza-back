package com.icpizza.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record ShiftEventResponse(
        String status,        // "created"
        String id,              // event id
        @JsonProperty("shiftNo") Integer shiftNo,
        CashWarning cashWarning
){
    public record CashWarning(String error, BigDecimal expected) {}

}
