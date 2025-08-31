package com.icpizza.backend.mapper;

import com.icpizza.backend.cache.MenuSnapshot;
import com.icpizza.backend.dto.CreateOrderTO;
import com.icpizza.backend.dto.OrderHistoryTO;
import com.icpizza.backend.entity.Customer;
import com.icpizza.backend.entity.Order;
import com.icpizza.backend.entity.OrderItem;
import com.icpizza.backend.enums.OrderStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class OrderMapper {
    Random random = new Random();
    private static final ZoneId BAHRAIN = ZoneId.of("Asia/Bahrain");

    public Order toOrderEntity(CreateOrderTO orderTO, Customer customer){
        Order order = new Order();
        order.setAmountPaid(orderTO.amountPaid());
        order.setOrderNo(random.nextInt(1, 999));
        order.setType(orderTO.orderType());
        order.setNotes(orderTO.notes());
        order.setCustomer(customer);
        order.setPaymentType(orderTO.paymentType());
        order.setStatus(OrderStatus.toLabel(OrderStatus.KITCHEN_PHASE));
        order.setCreatedAt(LocalDateTime.now(BAHRAIN));
        order.setExternalId(null);
        order.setAddress(orderTO.address());

        return order;
    }

    public List<OrderItem> toOrderItems(CreateOrderTO orderTO, Order order){
        List<OrderItem> orderItems = new ArrayList<>();

        for (CreateOrderTO.OrderItemsTO item: orderTO.orderItems()){
            OrderItem newItem = new OrderItem();
            newItem.setName(item.name());
            newItem.setOrder(order);
            newItem.setSize(item.size());
            newItem.setThinDough(Boolean.TRUE.equals(item.isThinDough()));
            newItem.setGarlicCrust(Boolean.TRUE.equals(item.isGarlicCrust()));
            newItem.setQuantity(item.quantity());
            newItem.setAmount(item.amount());
            newItem.setCategory(item.category());
            newItem.setDescription(item.description());
            newItem.setDiscountAmount(item.discountAmount());

            orderItems.add(newItem);
        }

        return orderItems;
    }

    public CreateOrderTO toCreateOrderTO(Order order, List<OrderItem> items) {
        return new CreateOrderTO(
                order.getId(),
                order.getCustomer().getTelephoneNo(),
                order.getCustomer().getName(),
                Long.valueOf(order.getCustomer().getId()),
                order.getAmountPaid(),
                toOrderItemsTO(items),
                order.getPaymentType(),
                order.getType(),
                order.getNotes(),
                order.getAddress()
        );
    }

    public List<CreateOrderTO.OrderItemsTO> toOrderItemsTO(List<OrderItem> items){
        if (items == null || items.isEmpty()) return java.util.List.of();

        List<CreateOrderTO.OrderItemsTO> res = new java.util.ArrayList<>(items.size());
        for (OrderItem it : items) {
            res.add(new CreateOrderTO.OrderItemsTO(
                    it.getAmount(),
                    it.getCategory(),
                    it.getDescription(),
                    it.getDiscountAmount(),
                    it.isGarlicCrust(),
                    it.isThinDough(),
                    it.getName(),
                    it.getQuantity() == null ? 0 : it.getQuantity(),
                    it.getSize()
            ));
        }
        return res;
    }

    public OrderHistoryTO toOrderHistoryTO(Order order, List<OrderItem> items, MenuSnapshot menu){
        List<OrderHistoryTO.OrderItemHistoryTO> itemHistoryTOList = items
                .stream()
                .map(item -> toOrderHistoryItemTO(item, menu))
                .toList();

        Customer customer = order.getCustomer();

        String customerName = (customer != null && customer.getName() != null) ? customer.getName() : "Unknown customer";

        String telephoneNo = (customer !=null ? customer.getTelephoneNo(): "Failed to fetch number");

        var sale = itemHistoryTOList.stream()
                .map(OrderHistoryTO.OrderItemHistoryTO::discountAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
                .setScale(2, java.math.RoundingMode.HALF_UP);

        return new OrderHistoryTO(
                order.getId(),
                order.getOrderNo(),
                order.getType(),
                order.getAmountPaid(),
                telephoneNo,
                sale,
                customerName,
                order.getCreatedAt().toString(),
                order.getPaymentType(),
                order.getNotes(),
                itemHistoryTOList
        );
    }

    public OrderHistoryTO.OrderItemHistoryTO toOrderHistoryItemTO(OrderItem item, MenuSnapshot menu){
        return new OrderHistoryTO.OrderItemHistoryTO(
                item.getName(),
                item.getQuantity(),
                item.getAmount(),
                item.getSize(),
                item.getCategory(),
                item.isGarlicCrust(),
                item.isThinDough(),
                item.getDescription(),
                item.getDiscountAmount(),
                photoByName(menu, item.getName())
        );
    }

    private String photoByName(MenuSnapshot snap, String name) {
        if (snap == null || name == null) return "";
        // используем публичный аксессор списка из снапшота
        return snap.getItems().stream()
                .filter(mi -> name.equals(mi.getName()))
                .map(com.icpizza.backend.entity.MenuItem::getPhoto)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse("");
    }
}
