package com.icpizza.backend.dto.menu;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.icpizza.backend.dto.order.UserInfoDTO;

import java.util.List;

public record MenuResponse(
        @JsonProperty("menu")
        List<MenuItemDTO> mainMenu,

        @JsonProperty("extraIngr")
        List<ExtraIngrDTO> extras,

        List<ToppingsDTO> toppings,
        @JsonProperty("userInfo")
        UserInfoDTO userInfo
) {
}
