package com.icpizza.backend.mapper;

import com.icpizza.backend.cache.MenuSnapshot;
import com.icpizza.backend.dto.menu.ExtraIngrDTO;
import com.icpizza.backend.dto.menu.MenuItemDTO;
import com.icpizza.backend.dto.menu.MenuResponse;
import com.icpizza.backend.dto.menu.ToppingsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MenuMapper {
    public MenuResponse toBaseInfoSnakeCase(MenuSnapshot menu){
        List<MenuItemDTO> menuList = menu.getItems().stream()
                .map(mi -> {
                    return new MenuItemDTO(
                            mi.getId(),
                            mi.getName(),
                            mi.getCategory(),
                            mi.getSize(),
                            mi.getPhoto(),
                            mi.getDescription(),
                            mi.isAvailable(),
                            mi.isBestSeller(),
                            mi.getPrice()
                            );
                }).toList();

        List<ExtraIngrDTO> extras = menu.getExtras().stream()
                .map(extraIngr -> {
                    return new ExtraIngrDTO(
                            extraIngr.getId(),
                            extraIngr.getName(),
                            extraIngr.getPhoto(),
                            extraIngr.isAvailable(),
                            extraIngr.getSize(),
                            extraIngr.getPrice()
                    );
                }).toList();

        List<ToppingsDTO> toppings = menu.getToppings().stream().map(
                topping -> {
                    return new ToppingsDTO(
                            topping.getId(),
                            topping.getPhoto(),
                            topping.getName(),
                            topping.getAvailable(),
                            topping.getPrice()

                    );
                }).toList();

        return new MenuResponse(menuList,extras,toppings, null);
    }
}
