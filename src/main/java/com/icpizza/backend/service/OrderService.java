package com.icpizza.backend.service;

import com.icpizza.backend.cache.MenuSnapshot;
import com.icpizza.backend.dto.*;
import com.icpizza.backend.entity.Customer;
import com.icpizza.backend.entity.MenuItem;
import com.icpizza.backend.entity.Order;
import com.icpizza.backend.entity.OrderItem;
import com.icpizza.backend.enums.OrderStatus;
import com.icpizza.backend.jahez.api.JahezApi;
import com.icpizza.backend.jahez.dto.JahezDTOs;
import com.icpizza.backend.jahez.mapper.JahezOrderMapper;
import com.icpizza.backend.mapper.OrderMapper;
import com.icpizza.backend.repository.CustomerRepository;
import com.icpizza.backend.repository.OrderItemRepository;
import com.icpizza.backend.repository.OrderRepository;
import com.icpizza.backend.repository.TransactionRepository;
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
import java.util.concurrent.ThreadLocalRandom;
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
    private final JahezOrderMapper jahezOrderMapper;
    private final JahezApi jahezApi;
    private final TransactionRepository transactionRepo;

    private static final List<String> CATEGORY_ORDER = List.of(
            "Combo Deals", "Brick Pizzas", "Pizzas", "Sides", "Sauces", "Beverages"
    );

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    static String safeName(String n) {
        if (n == null) return null;
        String v = n.trim();
        if (v.isEmpty()) return null;
        if ("no data".equalsIgnoreCase(v)) return null;
        return v;
    }

    static String coordsAsAddress(JahezDTOs.JahezOrderCreatePayload jahezOrder) {
        var d = jahezOrder.delivery_address();
        if (d == null || d.geolocation == null) return "";
        var g = d.geolocation;
        if (g.latitude == null || g.longitude == null) return "";
        return String.format(java.util.Locale.US, "%.6f,%.6f", g.latitude, g.longitude);
    }

    @Transactional
    public CreateOrderTO createWebsiteOrder(CreateOrderTO orderTO) {
        Boolean hasTelephone = orderTO.telephoneNo()==null? false: true;
        log.info(String.valueOf(orderTO));

        if (hasTelephone){
            Optional<Customer> customerOptional = customerRepo.findByTelephoneNo(orderTO.telephoneNo());
            Customer customer = customerOptional.orElseGet(() -> customerService.createCustomer(orderTO));


            Order order = orderMapper.toOrderEntity(orderTO, customer);
            orderRepo.saveAndFlush(order);
            log.info(String.valueOf(order));

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
        if (orderRepo.findByExternalId(jahezOrder.jahez_id()).isPresent()) {
            log.info("[JAHEZ] duplicate create jahez_id={}, skip", jahezOrder.jahez_id());
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
        orderRepo.saveAndFlush(order);

        var mapped = jahezOrderMapper.map(jahezOrder, order);
        orderItemRepo.saveAll(mapped.items());

        order.setAmountPaid(mapped.declaredTotal());
        orderRepo.save(order);

        if (mapped.priceMismatch()) {
            log.warn("[JAHEZ] price mismatch: declared={}, computed={}",
                    mapped.declaredTotal(), mapped.total());
        }

        orderEvents.pushCreated(order, mapped.items());
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
    public void updateOrderStatus(OrderStatusUpdateTO orderStatusUpdateTO) {
        log.info("[NEW ORDER]: "+orderStatusUpdateTO+" ");
        if (orderStatusUpdateTO.jahezOrderId() != null
                && (orderStatusUpdateTO.orderStatus().equals("Accepted")
                || orderStatusUpdateTO.orderStatus().equals("Rejected"))) {
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

                orderEvents.pushAccepted(orderStatusUpdateTO.orderId());
            }
            else{
                throw new IllegalArgumentException("Unsupported orderStatus for Jahez: " + st);
            }

            if (orderStatusUpdateTO.orderStatus().equals("Rejected")) {
                log.info("[JAHEZ] Order with id "+order.getId()+" rejected: "+orderStatusUpdateTO.reason()+".");
                String reason = (orderStatusUpdateTO.reason() == null || orderStatusUpdateTO.reason().isBlank())
                        ? "Rejected by operator" : orderStatusUpdateTO.reason();


                jahezApi.sendRejected(extId, reason)
                        .timeout(Duration.ofMinutes(5))
                        .retry(1)
                        .doOnError(e -> log.error("Jahez REJECT failed extId={}, reason={}", extId, reason, e))
                        .subscribe();


                try { orderItemRepo.deleteByOrderId(order.getId()); } catch (Exception ignore) {}
                orderEvents.pushDeleted(order.getId());
                orderRepo.delete(order);
            }
            else{
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

            order.setIsReady(true);

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

            order.setIsPickedUp(true);

            orderRepo.save(order);

            orderEvents.pushPickedUp(order.getId());
        }
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
        List<Order> orders = orderRepo.findActiveOrders();
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
                    o.getIsReady(),
                    o.getIsPaid(),
                    o.getIsPickedUp(),
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

        order.setIsReady(true);

        Integer duration = (int) Math.max(0, Duration.between(order.getCreatedAt(), LocalDateTime.now(BAHRAIN)).getSeconds());

        order.setReadyTimeStamp(duration);
        order.setStatus(OrderStatus.toLabel(OrderStatus.READY));

        orderRepo.save(order);

        orderPostProcessor.onOrderReady(new OrderPostProcessor.OrderReadyEvent(order));
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

        orderItemRepo.deleteAllByOrderId(order.getId());
        orderRepo.deleteById(order.getId());

        if(customer!=null){
            customer.setAmountOfOrders(customer.getAmountOfOrders()-1);
            if(customer.getAmountOfOrders()==0){
                customerRepo.delete(customer);
                return "Order "+orderId+" was successfully deleted";
            }
            customer.setAmountPaid(customer.getAmountPaid().subtract(order.getAmountPaid()));
            customerRepo.save(customer);
        }

        return "Order "+orderId+" was successfully deleted";
    }

    public final class Bd {
        private Bd() {}
        public static BigDecimal nz(BigDecimal x) { return x == null ? BigDecimal.ZERO : x; }
    }

    public Map<String, List<OrderHistoryTO>> getHistory(){
        List<Order> orders = orderRepo.findReadySince(LocalDateTime.now(BAHRAIN).minusDays(1));

        MenuSnapshot snap = menuService.getMenu();

        List<OrderHistoryTO> orderHistory = orders
                .stream()
                .map(order -> orderMapper.toOrderHistoryTO(order, orderItemRepo.findByOrderId(order.getId()), snap))
                .toList();

        return Map.of("orders", orderHistory);
    }
}

