package com.icpizza.backend.dto;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PriorityUpdateRequest(
        @JsonProperty("orderIds")
        List<Long> orders
) {
}
