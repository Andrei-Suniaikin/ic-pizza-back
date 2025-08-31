package com.icpizza.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UpdatePaymentTypeTO(
        @JsonProperty("order_id")
        Long id,
        @JsonProperty("payment_type")
        String paymentType
) {
}
