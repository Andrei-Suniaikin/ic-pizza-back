package com.icpizza.backend.service;

import com.icpizza.backend.entity.Order;
import com.icpizza.backend.entity.OrderItem;
import com.icpizza.backend.repository.OrderItemRepository;
import com.icpizza.backend.repository.OrderRepository;
import com.icpizza.backend.tiktok.service.TikTokService;
import com.icpizza.backend.websocket.OrderEvents;
import com.icpizza.backend.whatsapp.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPostProcessor {
    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final WhatsAppService wa;
    private final OrderEvents orderEvents;
    private final TikTokService tikTokService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW,readOnly = true)
    public void onOrderCreated(OrderCreatedEvent event) {
        String kitchenMsg = wa.buildKitchenMessage(event.items);

        String tel = event.order.getCustomer() != null ? event.order.getCustomer().getTelephoneNo() : null;
        String name = (event.order.getCustomer() != null ? event.order.getCustomer().getName() : null);
        if (tel != null && !tel.isBlank() && !"Unknown customer".equalsIgnoreCase(tel)) {
            String clientMsg = wa.buildOrderMessage(event.order.getId(), event.items, event.order.getAmountPaid());
            wa.sendOrderConfirmation(tel, clientMsg, event.order.getAmountPaid(), event.order.getId());
        }

        wa.sendOrderToKitchenText2(event.order.getOrderNo().longValue(), kitchenMsg, tel, false, name);

        orderEvents.pushCreated(event.order, event.items);

        tikTokService.sendPlaceAnOrder(event.order.getCustomer().getTelephoneNo(), event.order.getAmountPaid());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW,readOnly = true)
    public void onOrderEdited(OrderEditedEvent event){
        String kitchenMsg = wa.buildKitchenMessage(event.items);

        String tel  = event.order.getCustomer() != null ? event.order.getCustomer().getTelephoneNo() : null;
        String name = event.order.getCustomer() != null ? event.order.getCustomer().getName() : null;

        Integer headerNumber = event.order.getOrderNo(); // или: order.getId().intValue()
        wa.sendOrderToKitchenText2(Long.valueOf(headerNumber), kitchenMsg, tel, true, name);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderReady(OrderReadyEvent orderReadyEvent){
        try {
            if (orderReadyEvent.order.getCustomer().getTelephoneNo() != null
                    && !orderReadyEvent.order.getCustomer().getTelephoneNo().isBlank()) {
                wa.sendReadyMessage(orderReadyEvent.order.getCustomer().getTelephoneNo(),
                        orderReadyEvent.order.getCustomer().getName(),
                        orderReadyEvent.order.getCustomer().getId());
            }
        } catch (Exception ex) {
            log.error("[ready] WhatsApp send failed", ex);
        }
    }

    public record OrderCreatedEvent(Order order, List<OrderItem> items) {}
    public record OrderEditedEvent(Order order, List<OrderItem> items) {}
    public record OrderReadyEvent(
            Order order
    ) {}
}


