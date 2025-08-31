package com.icpizza.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MarkOrderReadyTO(
        @JsonProperty("orderId")
        Long id
) {
}
