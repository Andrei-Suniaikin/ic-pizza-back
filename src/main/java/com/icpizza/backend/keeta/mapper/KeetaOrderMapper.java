package com.icpizza.backend.keeta.mapper;

import com.icpizza.backend.dto.CreateOrderTO;
import com.icpizza.backend.entity.*;
import com.icpizza.backend.keeta.dto.CreateKeetaOrderTO;
import com.icpizza.backend.repository.BranchRepository;
import com.icpizza.backend.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class KeetaOrderMapper {
    private final BranchRepository branchRepository;
    private final BranchService branchService;
    Random random = new Random();

    public Order toOrderEntity(CreateKeetaOrderTO createKeetaOrderTO, Customer customer) {
        Order order = new Order();
        Branch branch = branchRepository.findById(createKeetaOrderTO.branchId())
                .orElseThrow(RuntimeException::new);
        order.setExternalId(Long.valueOf(createKeetaOrderTO.orderId()));
        order.setCreatedAt(createKeetaOrderTO.createdAt());
        order.setBranch(branch);
        order.setType("Keeta");
        order.setOrderNo(random.nextInt(1, 999));
        order.setAmountPaid(createKeetaOrderTO.amountPaid());
        order.setEstimation(branchService.getEstimationByBranch(branch));
        order.setNotes(createKeetaOrderTO.description());
        order.setCustomer(customer);
        order.setAddress(createKeetaOrderTO.address());
        order.setPaymentType(createKeetaOrderTO.paymentType());
        order.setStatus("Kitchen Phase");

        return order;
    }

    public List<OrderItem> toOrderItemEntity(List<CreateKeetaOrderTO.OrderItemTO> items, Order order) {
        List<OrderItem> orderItems = new ArrayList<>();
        for (CreateKeetaOrderTO.OrderItemTO item : items) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setName(item.name());
            orderItem.setAmount(item.price());
            orderItem.setQuantity(item.quantity());
            orderItem.setGarlicCrust(item.isGarlicCrust());
            orderItem.setThinDough(item.isThinDough());
            orderItem.setSize(item.size());
            orderItem.setCategory(item.category());
            orderItem.setDescription(item.description());
            orderItem.setDiscountAmount(BigDecimal.ZERO);
            orderItems.add(orderItem);
        }

        return orderItems;
    }

    public List<ComboItem> toComboItemEntity(List<CreateKeetaOrderTO.OrderItemTO> orderItemsTO, List<OrderItem> itemEntities) {
        List<ComboItem> mappedComboItems = new ArrayList<>();

        if (orderItemsTO == null || itemEntities == null || orderItemsTO.size() != itemEntities.size()) {
            return mappedComboItems;
        }

        for(int i=0; i< orderItemsTO.size(); i++){
            CreateKeetaOrderTO.OrderItemTO itemTO = orderItemsTO.get(i);
            OrderItem orderItem = itemEntities.get(i);

            if(itemTO.comboItemTOList() != null && !itemTO.comboItemTOList().isEmpty()){
                List<ComboItem> comboItems = itemTO.comboItemTOList().stream()
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
}
