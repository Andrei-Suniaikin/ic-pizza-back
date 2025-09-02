package com.icpizza.backend.service;

import com.icpizza.backend.cache.MenuSnapshot;
import com.icpizza.backend.config.TimeConfig;
import com.icpizza.backend.dto.*;
import com.icpizza.backend.entity.Customer;
import com.icpizza.backend.entity.MenuItem;
import com.icpizza.backend.entity.Order;
import com.icpizza.backend.entity.OrderItem;
import com.icpizza.backend.enums.OrderStatus;
import com.icpizza.backend.jahez.api.JahezApi;
import com.icpizza.backend.jahez.dto.JahezDTOs;
import com.icpizza.backend.jahez.mapper.JahezOrderItemMapper;
import com.icpizza.backend.mapper.OrderMapper;
import com.icpizza.backend.repository.CustomerRepository;
import com.icpizza.backend.repository.OrderItemRepository;
import com.icpizza.backend.repository.OrderRepository;
import com.icpizza.backend.websocket.OrderEvents;
import jakarta.persistence.EntityNotFoundException;
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
import java.util.concurrent.CompletableFuture;
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
    Random random = new Random();
    private final JahezOrderItemMapper jahezOrderItemMapper;
    private final JahezApi jahezApi;

    private static final List<String> CATEGORY_ORDER = List.of(
            "Combo Deals", "Brick Pizzas", "Pizzas", "Sides", "Sauces", "Beverages"
    );

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Transactional
    public CreateOrderTO createWebsiteOrder(CreateOrderTO orderTO) {
        Boolean hasTelephone = orderTO.telephoneNo()==null? false: true;

        if (hasTelephone){
            Optional<Customer> customerOptional = customerRepo.findByTelephoneNo(orderTO.telephoneNo());
            Customer customer = customerOptional.orElseGet(() -> customerService.createCustomer(orderTO));


            Order order = orderMapper.toOrderEntity(orderTO, customer);
            orderRepo.saveAndFlush(order);

            List<OrderItem> orderItems = orderMapper.toOrderItems(orderTO, order);
            orderItemRepo.saveAll(orderItems);

            customerOptional.ifPresent(c -> customerService.updateCustomer(order, customer));

            orderPostProcessor.onOrderCreated(new OrderPostProcessor.OrderCreatedEvent(order, orderItems));

            return orderMapper.toCreateOrderTO(order, orderItems);
        }

        Customer customer = null;

        Order order = orderMapper.toOrderEntity(orderTO, customer);
        orderRepo.saveAndFlush(order);

        List<OrderItem> orderItems = orderMapper.toOrderItems(orderTO, order);
        orderItemRepo.saveAll(orderItems);

        orderPostProcessor.onOrderCreated(new OrderPostProcessor.OrderCreatedEvent(order, orderItems));

        return orderMapper.toCreateOrderTO(order, orderItems);
    }

    @Async
    @Transactional
    public void createJahezOrder(JahezDTOs.JahezOrderCreatePayload jahezOrder){
        try {
            if (orderRepo.findByExternalId(jahezOrder.jahez_id()).isPresent()) {
                CompletableFuture.completedFuture(true);
                return;
            }

            Order order = new Order();
            order.setStatus(OrderStatus.toLabel(OrderStatus.PENDING));
            order.setOrderNo(random.nextInt(1, 999));
            order.setAddress(null);
            order.setExternalId(jahezOrder.jahez_id());
            order.setNotes(jahezOrder.notes());
            order.setType("Jahez");
            order.setCreatedAt(LocalDateTime.now(BAHRAIN));
            order.setPaymentType(JahezDTOs.JahezOrderCreatePayload.PaymentMethod.toLabel(jahezOrder.payment_method()));
            order.setAmountPaid(jahezOrder.final_price());
            order.setNotes(jahezOrder.notes());

            orderRepo.save(order);

            List<OrderItem> items = jahezOrderItemMapper.toOrderItem(jahezOrder, order);
            orderItemRepo.saveAll(items);

            orderEvents.pushCreated(order, items);
        } catch (Exception e) {
            log.error("createJahezOrder failed " + jahezOrder);
            throw new RuntimeException(e);
        }
    }

    @Transactional
    @Async
    public void updateJahezOrderStatus(JahezDTOs.JahezOrderUpdatePayload payload){
        Order order = orderRepo.findByExternalId(payload.jahezOrderId())
                .orElseThrow(()-> new IllegalArgumentException("No order found with externalId: " + payload.jahezOrderId()));

        order.setPaymentType(JahezDTOs.JahezOrderCreatePayload.PaymentMethod.toLabel(payload.payment_method()));
        order.setStatus(OrderStatus.toLabel(payload.status()));
    }

    @Transactional
    public Order updateOrderStatus(OrderStatusUpdateTO orderStatusUpdateTO) {
        if (orderStatusUpdateTO.jahezOrderId() != null) {
            final long extId = orderStatusUpdateTO.jahezOrderId();
            final String st = orderStatusUpdateTO.orderStatus();

            Order order = orderRepo.findByExternalId(extId)
                    .orElseThrow(() -> new IllegalArgumentException("Order by jahezId not found: " + extId));

            if (orderStatusUpdateTO.orderStatus().equals("Accepted")) {
                if (!order.getStatus().equals(OrderStatus.toLabel(OrderStatus.KITCHEN_PHASE))) {
                    order.setStatus(OrderStatus.toLabel(OrderStatus.KITCHEN_PHASE));
                    orderRepo.saveAndFlush(order);
                }
                jahezApi.sendAccepted(extId)
                        .timeout(Duration.ofSeconds(5))
                        .retry(1)
                        .doOnError(e -> log.error("Jahez ACCEPT failed extId={}", extId, e))
                        .subscribe();
                return order;
            }

            if (orderStatusUpdateTO.orderStatus().equals("Rejected")) {
                String reason = (orderStatusUpdateTO.reason() == null || orderStatusUpdateTO.reason().isBlank())
                        ? "Rejected by operator" : orderStatusUpdateTO.reason();


                jahezApi.sendRejected(extId, reason)
                        .timeout(Duration.ofSeconds(5))
                        .retry(1)
                        .doOnError(e -> log.error("Jahez REJECT failed extId={}, reason={}", extId, reason, e))
                        .subscribe();


                try { orderItemRepo.deleteByOrderId(order.getId()); } catch (Exception ignore) {}
                orderRepo.delete(order);
                return null;
            }

            throw new IllegalArgumentException("Unsupported orderStatus for Jahez: " + st);
        }

        // 2) Ветка внутренних заказов: только READY по orderId
        if (!"READY".equalsIgnoreCase(orderStatusUpdateTO.orderStatus())) {
            throw new IllegalArgumentException("Only READY allowed when jahezOrderId is null");
        }
        if (orderStatusUpdateTO.orderId() == null) {
            throw new IllegalArgumentException("orderId is required for READY");
        }

        Order order = orderRepo.findById(orderStatusUpdateTO.orderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderStatusUpdateTO.orderId()));
        if (order.getStatus() != OrderStatus.toLabel(OrderStatus.READY)) {
            order.setStatus(OrderStatus.toLabel(OrderStatus.READY));
            orderRepo.save(order);
        }

        return order;
    }

    @Transactional
    public void editOrder(EditOrderTO editOrderTO) {
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
        orderToEdit.setPaymentType(editOrderTO.paymentType());
        orderToEdit.setNotes(editOrderTO.notes());
        orderToEdit.setAmountPaid(editOrderTO.amountPaid());
        orderRepo.save(orderToEdit);

        orderItemRepo.deleteByOrderId(orderToEdit.getId());
        List<OrderItem> savedItems = orderItemRepo.saveAll(
                (editOrderTO.items() == null ? List.<EditOrderTO.EditOrderItemTO>of() : editOrderTO.items())
                        .stream()
                        .map(it -> {
                            OrderItem oi = new OrderItem();
                            oi.setOrder(orderToEdit);
                            oi.setName(it.name());
                            oi.setQuantity(it.quantity());
                            oi.setAmount((it.amount()));
                            oi.setSize(it.size());
                            oi.setCategory(it.category());
                            oi.setGarlicCrust(Boolean.TRUE.equals(it.isGarlicCrust()));
                            oi.setThinDough(Boolean.TRUE.equals(it.isThinDough()));
                            oi.setDescription(it.description());
                            oi.setDiscountAmount(it.discountAmount());
                            return oi;
                        }).toList()
        );

        orderItemRepo.saveAll(savedItems);

        orderPostProcessor.onOrderEdited(new OrderPostProcessor.OrderEditedEvent(orderToEdit, savedItems));
    }

    public Map<String, List<ActiveOrdersTO>> getAllActiveOrders() {
        List<Order> orders = orderRepo.findWithCustomerByStatus(OrderStatus.toLabel(OrderStatus.KITCHEN_PHASE));
        if (orders.isEmpty()) return Map.of("orders", List.of());

        var ids = orders.stream().map(Order::getId).toList();
        var allItems = orderItemRepo.findByOrderIdIn(ids);
        var itemsByOrder = allItems.stream()
                .collect(Collectors.groupingBy(oi -> oi.getOrder().getId()));

        var menu = safeMenu();

        List<ActiveOrdersTO> result = orders.stream().map(o -> {
            var oiList = itemsByOrder.getOrDefault(o.getId(), List.of());

            List<ActiveOrdersTO.ActiveOrderItemTO> itemTOs = oiList.stream().map(oi -> new ActiveOrdersTO.ActiveOrderItemTO(
                    oi.getName(),
                    oi.getQuantity(),
                    oi.getAmount(),
                    oi.getSize(),
                    oi.getCategory(),
                    oi.isGarlicCrust(),
                    oi.isThinDough(),
                    oi.getDescription(),
                    oi.getDiscountAmount(),
                    photoByName(menu, oi.getName())
            )).toList();

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
                    itemTOs
            );
        }).toList();

        log.info(result.toString());
        return Map.of("orders", result);
    }
    private MenuSnapshot safeMenu() {
        try { return menuService.getMenu(); } catch (Exception e) { return null; }
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
    public void markOrderReady(MarkOrderReadyTO markOrderReadyTO) {
        Order order = orderRepo.findById(markOrderReadyTO.id())
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order %d not found"
                        .formatted(markOrderReadyTO.id())));

        order.setStatus(OrderStatus.toLabel(OrderStatus.READY));

        orderRepo.save(order);

        orderPostProcessor.onOrderReady(new OrderPostProcessor.OrderReadyEvent(order));
    }

    public final class Bd {
        private Bd() {}
        public static BigDecimal nz(BigDecimal x) { return x == null ? BigDecimal.ZERO : x; }
    }

    public Map<String, List<OrderHistoryTO>> getHistory(){
        List<Order> orders = orderRepo.findReadySince(OrderStatus.toLabel(OrderStatus.READY),
                                                    LocalDateTime.now(BAHRAIN).minusDays(1));

        MenuSnapshot snap = menuService.getMenu();

        List<OrderHistoryTO> orderHistory = orders
                .stream()
                .map(order -> orderMapper.toOrderHistoryTO(order, orderItemRepo.findByOrderId(order.getId()), snap))
                .toList();

        return Map.of("orders", orderHistory);
    }
}

