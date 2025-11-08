package com.icpizza.backend.websocket.mapper;

import com.icpizza.backend.cache.MenuSnapshot;
import com.icpizza.backend.entity.Order;
import com.icpizza.backend.entity.OrderItem;
import com.icpizza.backend.repository.ComboItemRepository;
import com.icpizza.backend.websocket.dto.OrderPushTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class WebsocketOrderMapper {
    private final ComboItemRepository comboItemRepository;
    private static final DateTimeFormatter OUT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public List<OrderPushTO.ItemTO.ComboItemTO> getComboItems(OrderItem item){
        return comboItemRepository.findByOrderItemId(item.getId()).stream()
                .map(comboItem ->
                        new OrderPushTO.ItemTO.ComboItemTO(
                                comboItem.getCategory(),
                                comboItem.getName(),
                                comboItem.getSize(),
                                comboItem.isGarlicCrust(),
                                comboItem.isThinDough(),
                                comboItem.getQuantity(),
                                comboItem.getDescription()
                        )).toList();
    }

    public OrderPushTO toPush(Order order, List<OrderItem> items, MenuSnapshot menu) {
        BigDecimal sale = items.stream()
                .map(oi -> oi.getDiscountAmount() == null ? BigDecimal.ZERO : oi.getDiscountAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);



        List<OrderPushTO.ItemTO> itemTOs = items.stream().map(oi ->
                new OrderPushTO.ItemTO(
                        oi.getName(),
                        oi.getQuantity(),
                        oi.getAmount(),
                        oi.getSize(),
                        oi.getCategory(),
                        oi.getDescription(),
                        Boolean.TRUE.equals(oi.isGarlicCrust()),
                        Boolean.TRUE.equals(oi.isThinDough()),
                        oi.getDiscountAmount() == null ? BigDecimal.ZERO : oi.getDiscountAmount(),
                        photoByName(menu, oi.getName()),
                        getComboItems(oi)
                )
        ).toList();

        String customerName = order.getCustomer() != null ? order.getCustomer().getName() : "";
        String tel = order.getCustomer() != null ? order.getCustomer().getTelephoneNo() : "rabotyaga";

        return new OrderPushTO(
                order.getId(),
                order.getOrderNo(),
                order.getType(),
                order.getAmountPaid(),
                tel,
                order.getAddress(),
                sale,
                customerName,
                order.getCreatedAt() == null ? "" : OUT_FMT.format(order.getCreatedAt()),
                order.getPaymentType(),
                order.getNotes() == null ? "" : order.getNotes(),
                order.getExternalId(),
                itemTOs,
                order.getIsPaid(),
                order.getStatus(),
                order.getEstimation()
        );
    }


    private String photoByName(MenuSnapshot snap, String name) {
        if (snap == null || name == null) return "";
        return snap.getItems().stream()
                .filter(mi -> name.equals(mi.getName()))
                .map(com.icpizza.backend.entity.MenuItem::getPhoto)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse("");
    }

}
