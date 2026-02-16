package com.icpizza.backend.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record UserInfoDTO(
        String name,
        @JsonProperty("phone")
        String telephoneNo
) {
}
