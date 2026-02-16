package com.icpizza.backend.service;

import com.icpizza.backend.cache.MenuSnapshot;
import com.icpizza.backend.dto.branch.CashUpdateRequest;
import com.icpizza.backend.dto.order.*;
import com.icpizza.backend.entity.*;
import com.icpizza.backend.enums.CashUpdateType;
import com.icpizza.backend.enums.OrderStatus;
import com.icpizza.backend.jahez.api.JahezApi;
import com.icpizza.backend.jahez.dto.JahezDTOs;
import com.icpizza.backend.jahez.mapper.JahezOrderMapper;
import com.icpizza.backend.mapper.OrderMapper;
import com.icpizza.backend.repository.*;
import com.icpizza.backend.websocket.OrderEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final ZoneId BAHRAIN = ZoneId.of("Asia/Bahrain");
    private final OrderRepository orderRepo;
    private final CustomerRepository customerRepo;
    private final OrderItemRepository orderItemRepo;
    private final CustomerService customerService;
    private final OrderMapper orderMapper;
    private final MenuService menuService;
    private final OrderEvents orderEvents;
    private final OrderPostProcessor orderPostProcessor;
    private final JahezOrderMapper jahezOrderMapper;
    private final JahezApi jahezApi;
    private final TransactionRepository transactionRepo;
    private final ComboItemRepository comboItemRepo;
    private final BranchService branchService;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Transactional
    public CreateOrderResponse createWebsiteOrder(CreateOrderTO orderTO) {
        boolean hasTelephone = orderTO.telephoneNo() == null ? false : true;
        log.info("[CREATE WEBSITE ORDER] Creating new order from the website");

        Customer customer = null;

        if (hasTelephone) {
            Optional<Customer> customerOptional = customerRepo.findByTelephoneNo(orderTO.telephoneNo());
            customer = customerOptional.orElseGet(() -> customerService.createCustomer(orderTO));
        }
        log.info("[CREATE WEBSITE ORDER] {}", orderTO.branchId());
        Order order = orderMapper.toOrderEntity(orderTO, customer);
        branchService.recalcBranchWorkload(order.getBranch());
        order.setEstimation(branchService.getEstimationByBranch(order.getBranch()));
        orderRepo.saveAndFlush(order);

        List<OrderItem> orderItems = orderMapper.toOrderItems(orderTO, order);
        orderItemRepo.saveAllAndFlush(orderItems);

        List<ComboItem> comboItems = orderMapper.toComboItems(orderTO, orderItems);
        if (comboItems != null) comboItemRepo.saveAllAndFlush(comboItems);

        orderEvents.pushCreated(order, orderItems);
        log.info("[CREATE WEBSITE ORDER] Successfully created new order, {}", order);

        if (customer != null) {
            try {
                customerService.updateCustomer(order, customer);
            } catch (Exception e) {
                log.warn("Failed to update customer for order {}: {}", order.getId(), e.getMessage(), e);
            }
        }

        if (order.getPaymentType().equals("Cash")){
            branchService.cashUpdate(new CashUpdateRequest(
                    order.getBranch().getId(), CashUpdateType.CASH_IN, order.getAmountPaid(), "Order payment")
            );
        }

        orderPostProcessor.onOrderCreated(new OrderPostProcessor.OrderCreatedEvent(order, orderItems, comboItems));

        return orderMapper.toCreateOrderResponse(order, orderItems);
    }

    @Async
    @Transactional
    public void createJahezOrder(JahezDTOs.JahezOrderCreatePayload jahezOrder){
        log.info("[CREATE JAHEZE ORDER] Creating jahez order");
        if (orderRepo.findByExternalId(jahezOrder.jahez_id()).isPresent()) {
            log.info("[JAHEZ] duplicate create jahez_id={}, skip", jahezOrder.jahez_id());
            return;
        }

        Order order = jahezOrderMapper.toJahezOrderEntity(jahezOrder);
        branchService.recalcBranchWorkload(order.getBranch());
        order.setEstimation(branchService.getEstimationByBranch(order.getBranch()));
        orderRepo.saveAndFlush(order);

        var mapped = jahezOrderMapper.map(jahezOrder, order);
        orderItemRepo.saveAllAndFlush(mapped.items());

        order.setAmountPaid(mapped.total());
        orderRepo.save(order);

        if(mapped.comboItems()!=null) comboItemRepo.saveAllAndFlush(mapped.comboItems());

        branchService.recalcBranchWorkload(order.getBranch());

        orderEvents.pushCreated(order, mapped.items());
    }

    @Transactional
    @Async
    public void updateJahezOrderStatus(JahezDTOs.JahezOrderUpdatePayload payload){
        log.info("[JAHEZ ORDER STATUS UPDATE] payload={}", payload);
        Order order = orderRepo.findByExternalId(payload.jahezOrderId())
                .orElseThrow(()-> new IllegalArgumentException("No order found with externalId: " + payload.jahezOrderId()));

        order.setPaymentType(JahezDTOs.JahezOrderCreatePayload.PaymentMethod.toLabel(payload.payment_method()));
    }

    @Transactional
    public void updateOrderStatus(OrderStatusUpdateTO orderStatusUpdateTO) {
        log.info("[NEW ORDER]: "+orderStatusUpdateTO+" ");
        if (orderStatusUpdateTO.jahezOrderId() != null
                && (orderStatusUpdateTO.orderStatus().equals("Accepted")
                || orderStatusUpdateTO.orderStatus().equals("Cancelled"))) {
            final long extId = orderStatusUpdateTO.jahezOrderId();
            final String st = orderStatusUpdateTO.orderStatus();

            Order order = orderRepo.findByExternalId(extId)
                    .orElseThrow(() -> new IllegalArgumentException("Order by jahezId not found: " + extId));

            if (orderStatusUpdateTO.orderStatus().equals("Accepted")) {
                log.info("[JAHEZ] Order with id "+order.getId()+" accepted.");
                if (!order.getStatus().equals(OrderStatus.toLabel(OrderStatus.KITCHEN_PHASE))) {
                    order.setStatus(OrderStatus.toLabel(OrderStatus.KITCHEN_PHASE));
                    orderRepo.saveAndFlush(order);
                }
                jahezApi.sendAccepted(extId)
                        .timeout(Duration.ofMinutes(5))
                        .retry(1)
                        .doOnError(e -> log.error("Jahez ACCEPT failed extId={}", extId, e))
                        .subscribe();

                orderEvents.pushAccepted(orderStatusUpdateTO.orderId(), orderStatusUpdateTO.branchId());
            }
            else if (orderStatusUpdateTO.orderStatus().equals("Cancelled")) {
                log.info("[JAHEZ] Order with id "+order.getId()+" rejected: "+orderStatusUpdateTO.reason()+".");
                String reason = (orderStatusUpdateTO.reason() == null || orderStatusUpdateTO.reason().isBlank())
                        ? "Rejected by operator" : orderStatusUpdateTO.reason();


                jahezApi.sendRejected(extId, reason)
                        .timeout(Duration.ofMinutes(5))
                        .retry(1)
                        .doOnError(e -> log.error("Jahez REJECT failed extId={}, reason={}", extId, reason, e))
                        .subscribe();

                List<Long> orderItemIds = orderItemRepo.findIdsByOrderId(order.getId());
                if(!orderItemIds.isEmpty()) comboItemRepo.deleteByOrderItemIds(orderItemIds);
                try { orderItemRepo.deleteByOrderId(order.getId()); } catch (Exception ignore) {}
                orderRepo.delete(order);

                orderEvents.pushDeleted(order.getId(), orderStatusUpdateTO.branchId());
            }
            else {
                throw new IllegalArgumentException("Unsupported orderStatus for Jahez: " + st);
            }
        }

        if (orderStatusUpdateTO.orderId() == null) {
            throw new IllegalArgumentException("orderId is required for READY");
        }

        if(orderStatusUpdateTO.orderStatus().equals("Ready")) {
            Order order = orderRepo.findById(orderStatusUpdateTO.orderId())
                    .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order %d not found"
                            .formatted(orderStatusUpdateTO.orderId())));

            Integer duration = (int) Math.max(0, Duration.between(order.getCreatedAt(), LocalDateTime.now(BAHRAIN)).getSeconds());

            order.setReadyTimeStamp(duration);
            order.setStatus(OrderStatus.toLabel(OrderStatus.READY));

            orderRepo.save(order);

            orderPostProcessor.onOrderReady(new OrderPostProcessor.OrderReadyEvent(order));
        }

        if (orderStatusUpdateTO.orderStatus().equals("Picked Up")){
            Order order = orderRepo.findById(orderStatusUpdateTO.orderId())
                    .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order %d not found"
                            .formatted(orderStatusUpdateTO.orderId())));

            order.setStatus(OrderStatus.toLabel(OrderStatus.PICKED_UP));

            orderRepo.save(order);
            orderPostProcessor.onOrderPickedUp(new OrderPostProcessor.OrderPickedUpEvent(
                    (order.getCustomer() != null) ? order.getCustomer().getTelephoneNo() : null,
                    (order.getCustomer() != null) ? order.getCustomer().getName() : null,
                     order.getId(),
                    "Picked Up",
                    order.getBranch().getId()
                    ));        }

        if (orderStatusUpdateTO.orderStatus().equals("Oven")) {
            Order order = orderRepo.findById(orderStatusUpdateTO.orderId())
                    .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order %d not found"
                            .formatted(orderStatusUpdateTO.orderId())));

            order.setStatus(OrderStatus.toLabel(OrderStatus.OVEN));

            orderRepo.save(order);
            orderPostProcessor.onOrderInOven(new PushOrderStatusUpdated(orderStatusUpdateTO.orderId(), orderStatusUpdateTO.orderStatus(), order.getBranch().getId()));
        }
    }

    @Transactional
    public EditOrderResponse editOrder(EditOrderTO editOrderTO) {
        log.info("[ORDER EDIT] Editing order with id {}.", editOrderTO.orderId());
        Order orderToEdit = orderRepo.findById(editOrderTO.orderId())
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND, "Order %d not found".formatted(editOrderTO.orderId())));

        if(editOrderTO.telephoneNo()!=null){
            Customer customer = orderToEdit.getCustomer();
            if(customer==null) throw new IllegalArgumentException("Order has no customer");
            customer.setAmountPaid(customer.getAmountPaid()
                    .subtract(orderToEdit.getAmountPaid())
                    .add(editOrderTO.amountPaid()));
            customerRepo.save(customer);
        }

        if(orderToEdit.getPaymentType().equals("Cash") && !editOrderTO.paymentType().equals("Cash")) {
            branchService.cashUpdate(new CashUpdateRequest(
                    orderToEdit.getBranch().getId(), CashUpdateType.CASH_OUT, orderToEdit.getAmountPaid(), "Order payment refund")
            );
        }
        else if(!orderToEdit.getPaymentType().equals("Cash") && editOrderTO.paymentType().equals("Cash")) {
            branchService.cashUpdate(new CashUpdateRequest(
                    orderToEdit.getBranch().getId(), CashUpdateType.CASH_IN, orderToEdit.getAmountPaid(), "Order payment")
            );
        }

        orderToEdit.setPaymentType(editOrderTO.paymentType());
        orderToEdit.setNotes(editOrderTO.notes());
        orderToEdit.setAmountPaid(editOrderTO.amountPaid());
        orderRepo.save(orderToEdit);

        List<Long> orderItemIds = orderItemRepo.findIdsByOrderId(editOrderTO.orderId());
        comboItemRepo.deleteByOrderItemIds(orderItemIds);
        orderItemRepo.deleteByOrderId(orderToEdit.getId());

        List<OrderItem> savedItems = orderItemRepo.saveAll(
                orderMapper.toOrderItems(editOrderTO, orderToEdit)
        );
        orderItemRepo.saveAll(savedItems);

        List<ComboItem> allComboItems = new ArrayList<>();
        for (int i = 0; i < savedItems.size(); i++) {
            EditOrderTO.EditOrderItemTO itemTO = editOrderTO.items().get(i);
            OrderItem savedItem = savedItems.get(i);
            List<ComboItem> comboItems = orderMapper.toComboItems(itemTO, savedItem);
            allComboItems.addAll(comboItems);
        }
        comboItemRepo.saveAll(allComboItems);

        orderPostProcessor.onOrderEdited(new OrderPostProcessor.OrderEditedEvent(orderToEdit, savedItems, allComboItems));

        return new EditOrderResponse(orderToEdit.getId());
    }

    @Transactional(readOnly = true)
    public Map<String, List<ActiveOrdersTO>> getAllActiveOrders(UUID branchId) {
        List<Order> orders = orderRepo.findActiveOrders(branchId);
        if (orders.isEmpty()) return Map.of("orders", List.of());

        var ids = orders.stream().map(Order::getId).toList();
        var allItems = orderItemRepo.findByOrderIdIn(ids);
        var itemsByOrder = allItems.stream()
                .collect(Collectors.groupingBy(oi -> oi.getOrder().getId()));

        List<Long> orderItemIds = allItems.stream()
                .map(OrderItem::getId)
                .toList();
        List<ComboItem> allComboItems = comboItemRepo.findAllByOrderItemIdIn(orderItemIds);
        var comboItemsByOrderItem = allComboItems.stream()
                .collect(Collectors.groupingBy(ci -> ci.getOrderItem().getId()));
        var menu = safeMenu(branchId);

        List<ActiveOrdersTO> result = orders.stream().map(o -> {
            var oiList = itemsByOrder.getOrDefault(o.getId(), List.of());

            List<ActiveOrdersTO.ActiveOrderItemTO> itemTOs = oiList.stream().map(oi -> {
                var comboItems = comboItemsByOrderItem.getOrDefault(oi.getId(), List.of());

                List<ActiveOrdersTO.ActiveOrderItemTO.ActiveComboItemTO> comboTOs = comboItems.stream()
                        .map(ci -> new ActiveOrdersTO.ActiveOrderItemTO.ActiveComboItemTO(
                                ci.getName(),
                                ci.getCategory(),
                                ci.getSize(),
                                ci.getQuantity(),
                                ci.isGarlicCrust(),
                                ci.isThinDough(),
                                ci.getDescription()
                        ))
                        .toList();

                return new ActiveOrdersTO.ActiveOrderItemTO(
                        oi.getName(),
                        oi.getQuantity(),
                        oi.getAmount(),
                        oi.getSize(),
                        oi.getCategory(),
                        oi.isGarlicCrust(),
                        oi.isThinDough(),
                        oi.getDescription(),
                        oi.getDiscountAmount(),
                        photoByName(menu, oi.getName()),
                        comboTOs
                );
            }).toList();

            BigDecimal sale = itemTOs.stream()
                    .map(ActiveOrdersTO.ActiveOrderItemTO::discountAmount)
                    .map(Bd::nz)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String created = o.getCreatedAt() == null ? "" :
                    o.getCreatedAt()
                            .format(DT_FMT);

            String tel = o.getCustomer() == null ? null : o.getCustomer().getTelephoneNo();
            String custName = (o.getCustomer() != null && o.getCustomer().getName() != null)
                    ? o.getCustomer().getName() : null;

            return new ActiveOrdersTO(
                    o.getId(),
                    o.getExternalId(),
                    o.getOrderNo(),
                    o.getType(),
                    o.getAmountPaid(),
                    tel,
                    o.getAddress(),
                    sale,
                    custName,
                    created,
                    o.getPaymentType(),
                    o.getNotes() == null ? "" : o.getNotes(),
                    o.getStatus(),
                    o.getIsPaid(),
                    itemTOs,
                    o.getEstimation()
            );
        }).toList();

        log.info(result.toString());
        return Map.of("orders", result);
    }

    private MenuSnapshot safeMenu(UUID branchId) {
        try { return menuService.getMenu(branchId); } catch (Exception e) { return null; }
    }

    private String photoByName(MenuSnapshot snap, String name) {
        if (snap == null || name == null) return "";
        return snap.getItems().stream()
                .filter(mi -> name.equals(mi.getName()))
                .map(MenuItem::getPhoto)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("");
    }

    @Transactional
    public void updatePaymentType(UpdatePaymentTypeTO updatePaymentType) {
        Order order = orderRepo.findById(updatePaymentType.id())
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order %d not found"
                        .formatted(updatePaymentType.id())));

        order.setPaymentType(updatePaymentType.paymentType());

        orderRepo.save(order);
    }

    @Transactional
    public String deleteOrder(String orderId) {
        long id;
        try {
            id = Long.parseLong(orderId);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid orderId: " + orderId);
        }

        Order order = orderRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order " + id + " not found"));

        Customer customer = order.getCustomer();

        if(transactionRepo.existsByOrder(order)){
            transactionRepo.deleteByOrder(order);
        }

        if(order.getPaymentType().equals("Cash")) {
            branchService.cashUpdate(new CashUpdateRequest(
                    order.getBranch().getId(), CashUpdateType.CASH_OUT, order.getAmountPaid(), "Order payment refund")
            );
        }

        List<Long> orderItemIds = orderItemRepo.findIdsByOrderId(id);
        comboItemRepo.deleteByOrderItemIds(orderItemIds);
        orderItemRepo.deleteAllByOrderId(order.getId());
        orderRepo.deleteById(order.getId());

        if(customer!=null){
            customer.setAmountOfOrders(customer.getAmountOfOrders()-1);
            customer.setAmountPaid(customer.getAmountPaid().subtract(order.getAmountPaid()));
            customerRepo.save(customer);
        }

        return "Order "+orderId+" was successfully deleted";
    }

    @Transactional(readOnly = true)
    public OrderInfoTO getOrderInfo(Long orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order with id "+orderId+" wasn't found"));

        Integer estimation = branchService.getEstimationByBranch(order.getBranch());
        return orderMapper.toOrderInfoTO(order, estimation);
    }

    public final class Bd {
        private Bd() {}
        public static BigDecimal nz(BigDecimal x) { return x == null ? BigDecimal.ZERO : x; }
    }

    @Transactional(readOnly = true)
    public Map<String, List<OrderHistoryTO>> getHistory(UUID branchId) {
        List<Order> orders = orderRepo.findReadySince(branchId ,LocalDateTime.now(BAHRAIN).minusDays(1));

        MenuSnapshot snap = menuService.getMenu(branchId);

        List<OrderHistoryTO> orderHistory = orders
                .stream()
                .map(order -> orderMapper.toOrderHistoryTO(order, orderItemRepo.findByOrderId(order.getId()), snap))
                .toList();

        return Map.of("orders", orderHistory);
    }
}

