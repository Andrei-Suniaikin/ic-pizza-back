package com.icpizza.backend.mapper;

import com.icpizza.backend.cache.MenuSnapshot;
import com.icpizza.backend.dto.order.*;
import com.icpizza.backend.entity.*;
import com.icpizza.backend.enums.OrderStatus;
import com.icpizza.backend.repository.BranchRepository;
import com.icpizza.backend.repository.ComboItemRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class OrderMapper {
    private static final Logger log = LoggerFactory.getLogger(OrderMapper.class);
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    Random random = new Random();
    private static final ZoneId BAHRAIN = ZoneId.of("Asia/Bahrain");
    private final ComboItemRepository comboItemRepository;
    private final BranchRepository branchRepository;

    public Order toOrderEntity(CreateOrderTO orderTO, Customer customer){
        Order order = new Order();
        Branch branch = branchRepository.findById(orderTO.branchId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Illegal branch id"));
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
        order.setBranch(branch);
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

    public List<ComboItem> toComboItems(CreateOrderTO orderTO, List<OrderItem> orderItems){
        List<ComboItem> mappedComboItems = new ArrayList<>();
        for(int i=0; i< orderTO.orderItems().size(); i++){
            CreateOrderTO.OrderItemsTO itemTO = orderTO.orderItems().get(i);
            OrderItem orderItem = orderItems.get(i);

            if(itemTO.comboItems()!=null){
                List<ComboItem> comboItems = itemTO.comboItems().stream()
                        .map(comboItemTO -> {
                            ComboItem comboItem = new ComboItem();
                            comboItem.setOrderItem(orderItem);
                            comboItem.setName(comboItemTO.name());
                            comboItem.setCategory(comboItemTO.category());
                            comboItem.setSize(comboItemTO.size());
                            comboItem.setDescription(comboItemTO.description());
                            comboItem.setQuantity(comboItemTO.quantity());
                            comboItem.setGarlicCrust(Boolean.TRUE.equals(comboItemTO.isGarlicCrust()));
                            comboItem.setThinDough(Boolean.TRUE.equals(comboItemTO.isThinDough()));
                            return comboItem;
                        }).toList();

                mappedComboItems.addAll(comboItems);
            }
        }

        return mappedComboItems;
    }

    public List<OrderItem> toOrderItems(EditOrderTO editOrderTO, Order order) {
        if (editOrderTO.items() == null) return List.of();

        return editOrderTO.items().stream().map(it -> {
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setName(it.name());
            oi.setQuantity(it.quantity());
            oi.setAmount(it.amount());
            oi.setSize(it.size());
            oi.setCategory(it.category());
            oi.setGarlicCrust(Boolean.TRUE.equals(it.isGarlicCrust()));
            oi.setThinDough(Boolean.TRUE.equals(it.isThinDough()));
            oi.setDescription(it.description());
            oi.setDiscountAmount(it.discountAmount());
            return oi;
        }).toList();
    }

    public List<ComboItem> toComboItems(EditOrderTO.EditOrderItemTO itemTO, OrderItem orderItem) {
        if (itemTO.comboItems() == null) return List.of();

        return itemTO.comboItems().stream().map(ciTO -> {
            ComboItem ci = new ComboItem();
            ci.setOrderItem(orderItem);
            ci.setName(ciTO.name());
            ci.setCategory(ciTO.category());
            ci.setSize(ciTO.size());
            ci.setQuantity(ciTO.quantity());
            ci.setGarlicCrust(Boolean.TRUE.equals(ciTO.isGarlicCrust()));
            ci.setThinDough(Boolean.TRUE.equals(ciTO.isThinDough()));
            ci.setDescription(ciTO.description());
            return ci;
        }).toList();
    }

    public CreateOrderResponse toCreateOrderResponse(Order order, List<OrderItem> items) {
        Customer customer = order.getCustomer();
        String telephoneNo = (customer != null)
                ? order.getCustomer().getTelephoneNo()
                : null;
        String name = (customer != null)
                ? order.getCustomer().getName()
                : null;
        Boolean isNewCustomer = (customer != null)? customer.getAmountOfOrders()==1?Boolean.TRUE:Boolean.FALSE:Boolean.FALSE;
        log.info(name);
        return new CreateOrderResponse(
                order.getId(),
                telephoneNo,
                name,
                order.getAmountPaid(),
                toOrderItemsTO(items),
                order.getPaymentType(),
                order.getType(),
                order.getNotes(),
                order.getAddress(),
                order.getIsPaid(),
                order.getBranch().getBranchNumber(),
                isNewCustomer
        );
    }

    public List<CreateOrderTO.OrderItemsTO> toOrderItemsTO(List<OrderItem> items){
        if (items == null || items.isEmpty()) return java.util.List.of();

        List<CreateOrderTO.OrderItemsTO> res = new java.util.ArrayList<>(items.size());
        for (OrderItem it : items) {
            List<CreateOrderTO.OrderItemsTO.ComboItemsTO> comboItemsTOs = comboItemRepository
                    .findByOrderItemId(it.getId()).stream()
                    .map(ci -> new CreateOrderTO.OrderItemsTO.ComboItemsTO(
                            ci.getCategory(),
                            ci.getName(),
                            ci.getSize(),
                            ci.isGarlicCrust(),
                            ci.isThinDough(),
                            ci.getQuantity(),
                            ci.getDescription()
                    ))
                    .toList();

            res.add(new CreateOrderTO.OrderItemsTO(
                    it.getAmount(),
                    it.getCategory(),
                    it.getDescription(),
                    it.getDiscountAmount(),
                    it.isGarlicCrust(),
                    it.isThinDough(),
                    it.getName(),
                    it.getQuantity() == null ? 0 : it.getQuantity(),
                    it.getSize(),
                    comboItemsTOs
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
                order.getExternalId(),
                order.getNotes(),
                itemHistoryTOList
        );
    }

    public OrderHistoryTO.OrderItemHistoryTO toOrderHistoryItemTO(OrderItem item, MenuSnapshot menu){
        List<OrderHistoryTO.OrderItemHistoryTO.ComboItemHistoryTO> comboItemHistoryTOS = comboItemRepository.findByOrderItemId(item.getId()).stream()
                .map(comboItem ->
                    new OrderHistoryTO.OrderItemHistoryTO.ComboItemHistoryTO(
                            comboItem.getCategory(),
                            comboItem.getName(),
                            comboItem.getSize(),
                            comboItem.isGarlicCrust(),
                            comboItem.isThinDough(),
                            comboItem.getQuantity(),
                            comboItem.getDescription()
                    )
                ).toList();

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
                photoByName(menu, item.getName()),
                comboItemHistoryTOS
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

    public OrderInfoTO toOrderInfoTO(Order order, Integer estimation) {
        return new OrderInfoTO(
                order.getId(),
                order.getOrderNo(),
                order.getStatus(),
                order.getCreatedAt().format(DT_FMT),
                estimation
        );
    }
}
