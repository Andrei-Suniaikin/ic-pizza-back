package com.icpizza.backend.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MarkOrderReadyTO(
        @JsonProperty("orderId")
        Long id
) {
}
