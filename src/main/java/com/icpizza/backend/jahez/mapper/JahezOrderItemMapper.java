package com.icpizza.backend.jahez.mapper;

import com.icpizza.backend.entity.Order;
import com.icpizza.backend.entity.OrderItem;
import com.icpizza.backend.jahez.dto.JahezDTOs;
import com.icpizza.backend.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JahezOrderItemMapper {

    private final MenuService menuService;

    public List<OrderItem> toOrderItem(JahezDTOs.JahezOrderCreatePayload payload, Order order){
        List<OrderItem> orderItems = new ArrayList<>();

        for(JahezDTOs.JahezOrderCreatePayload.Item it: payload.products()){
            var meta = menuService.getItemDataForJahezOrderByExternalId(it.product_id());
            String name = meta.map(JahezDTOs.DataForJahezOrder::name).orElse(it.product_id());
            String category = meta.map(JahezDTOs.DataForJahezOrder::category).orElse("Error");
            String size = meta.map(JahezDTOs.DataForJahezOrder::size).orElse("Size Error");

            String extrasDesc = buildExtrasDescriptions(it);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setName(name);
            orderItem.setCategory(category);
            orderItem.setSize(size);
            orderItem.setQuantity(it.quantity());
            orderItem.setGarlicCrust(false);
            orderItem.setThinDough(false);
            orderItem.setDescription(extrasDesc);
            orderItem.setAmount(it.final_price());
            orderItem.setDiscountAmount(BigDecimal.ZERO);
            orderItems.add(orderItem);
        }

        return orderItems;
    }

    private String buildExtrasDescriptions(JahezDTOs.JahezOrderCreatePayload.Item it) {
        if (it.modifiers()==null) return "";
        List<String> description = new ArrayList<>();
        for(var modifier: it.modifiers()){
            if(modifier.options() == null) continue;
            for(var option: modifier.options()){
                String name = menuService.getExtraNameByExternalId(option.id()).orElse("Not found");
                int quantity = option.quantity();
                description.add(quantity>1? name + " x" + quantity : name);
            }
        }
        return  description.isEmpty() ? "" : "+" + String.join(" +", description);
    }
}
