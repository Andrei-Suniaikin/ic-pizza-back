package com.icpizza.backend.keeta.service;

import com.icpizza.backend.entity.ComboItem;
import com.icpizza.backend.entity.Customer;
import com.icpizza.backend.entity.Order;
import com.icpizza.backend.entity.OrderItem;
import com.icpizza.backend.keeta.dto.CreateKeetaOrderTO;
import com.icpizza.backend.keeta.mapper.KeetaOrderMapper;
import com.icpizza.backend.repository.ComboItemRepository;
import com.icpizza.backend.repository.CustomerRepository;
import com.icpizza.backend.repository.OrderItemRepository;
import com.icpizza.backend.repository.OrderRepository;
import com.icpizza.backend.service.BranchService;
import com.icpizza.backend.service.CustomerService;
import com.icpizza.backend.service.OrderPostProcessor;
import com.icpizza.backend.service.OrderService;
import com.icpizza.backend.websocket.OrderEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeetaOrderService {
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final KeetaOrderMapper keetaOrderMapper;
    private final BranchService branchService;
    private final OrderItemRepository orderItemRepository;
    private final ComboItemRepository comboItemRepository;
    private final OrderEvents orderEvents;
    private final OrderPostProcessor orderPostProcessor;
    private final OrderService orderService;

    @Transactional
    public String createKeetaOrder(CreateKeetaOrderTO createKeetaOrderTO) {
        try {
            log.info("[CREATE KEETA ORDER] Creating Keeta Order");
            Optional<Order> optionalOrder = orderRepository.findByExternalId(Long.valueOf(createKeetaOrderTO.orderId()));

            boolean hasTelephone = !createKeetaOrderTO.telephoneNo().contains("*");

            if (optionalOrder.isPresent() && createKeetaOrderTO.status().equals("50")) {
                Order order = optionalOrder.get();
                orderService.deleteOrder(String.valueOf(order.getId()));
                return "Success";
            }

            if (optionalOrder.isEmpty()) {
                Customer customer = null;

                if (hasTelephone) {
                    Optional<Customer> optionalCustomer = customerRepository.findByTelephoneNo(createKeetaOrderTO.telephoneNo());
                    customer = optionalCustomer.orElseGet(() -> customerService.createKeetaCustomer(createKeetaOrderTO));
                }

                Order order = keetaOrderMapper.toOrderEntity(createKeetaOrderTO, customer);
                branchService.recalcBranchWorkload(order.getBranch());
                order.setEstimation(branchService.getEstimationByBranch(order.getBranch()));
                orderRepository.saveAndFlush(order);

                List<OrderItem> orderItems = keetaOrderMapper.toOrderItemEntity(createKeetaOrderTO.items(), order);
                orderItemRepository.saveAllAndFlush(orderItems);

                List<ComboItem> comboItems = keetaOrderMapper.toComboItemEntity(createKeetaOrderTO.items(), orderItems);
                if (comboItems != null) comboItemRepository.saveAllAndFlush(comboItems);

                if(!createKeetaOrderTO.status().equals("40")) {
                    orderEvents.pushCreated(order, orderItems);
                    log.info("[CREATE WEBSITE ORDER] Successfully created new order, {}", order);
                }

                if (customer != null) {
                    try {
                        customerService.updateCustomer(order, customer);
                    } catch (Exception e) {
                        log.warn("Failed to update customer for order {}: {}", order.getId(), e.getMessage(), e);
                    }
                }
                if(!createKeetaOrderTO.status().equals("40")) {
                    orderPostProcessor.onOrderCreated(new OrderPostProcessor.OrderCreatedEvent(order, orderItems, comboItems));
                }
                return "success";
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to create keeta order", e);
        }
        return "error";
    }
}
