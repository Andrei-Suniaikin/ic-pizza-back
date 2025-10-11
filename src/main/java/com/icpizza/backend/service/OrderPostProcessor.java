package com.icpizza.backend.service;

import com.icpizza.backend.entity.Branch;
import com.icpizza.backend.entity.ComboItem;
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
    private final WhatsAppService wa;
    private final OrderEvents orderEvents;
    private final TikTokService tikTokService;
    private final BranchService branchService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW,readOnly = true)
    public void onOrderCreated(OrderCreatedEvent event) {
        String kitchenMsg = wa.buildKitchenMessage(event.items, event.comboItems);

        String tel = event.order.getCustomer() != null ? event.order.getCustomer().getTelephoneNo() : null;
        Branch branch = event.order.getBranch();
        Integer estimation = branchService.getEstimationByBranch(branch);
        String name = (event.order.getCustomer() != null ? event.order.getCustomer().getName() : null);
        if (tel != null && !tel.isBlank() && !"Unknown customer".equalsIgnoreCase(tel)) {
            String clientMsg = wa.buildOrderMessage(event.items, event.comboItems);
            wa.sendOrderConfirmation(tel, event.order,clientMsg);
            wa.sendEstimation(tel, estimation, event.order().getId());
            tikTokService.sendPlaceAnOrder(event.order.getCustomer().getTelephoneNo(), event.order.getAmountPaid());
        }

        wa.sendOrderToKitchenText2(event.order.getOrderNo(), kitchenMsg, tel, false, name);

        orderEvents.pushCreated(event.order, event.items);

    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW,readOnly = true)
    public void onOrderEdited(OrderEditedEvent event){
        String kitchenMsg = wa.buildKitchenMessage(event.items, event.comboItems);

        String tel  = event.order.getCustomer() != null ? event.order.getCustomer().getTelephoneNo() : null;
        String name = event.order.getCustomer() != null ? event.order.getCustomer().getName() : null;

        wa.sendOrderToKitchenText2(event.order.getOrderNo(), kitchenMsg, tel, true, name);

        orderEvents.pushUpdated(event.order, event.items);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderReady(OrderReadyEvent orderReadyEvent){
        try {
            if (orderReadyEvent.order.getCustomer()!= null
                    && !orderReadyEvent.order.getCustomer().getTelephoneNo().isBlank()) {
                wa.sendOrderReady(orderReadyEvent.order().getCustomer().getTelephoneNo());
            }
            orderEvents.pushReady(orderReadyEvent.order.getId());
        } catch (Exception ex) {
            log.error("[ready] WhatsApp send failed", ex);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPickedUp(OrderPickedUpEvent orderPickedUpEvent){
        try {
            if (orderPickedUpEvent.order.getCustomer()!= null
                    && !orderPickedUpEvent.order.getCustomer().getTelephoneNo().isBlank()) {
                wa.sendReadyMessage(orderPickedUpEvent.order().getCustomer().getTelephoneNo(),
                        orderPickedUpEvent.order.getCustomer().getName(),
                        orderPickedUpEvent.order.getCustomer().getId());
            }
            orderEvents.pushPickedUp(orderPickedUpEvent.order.getId());
        } catch (Exception ex) {
            log.error("[ready] WhatsApp send failed", ex);
        }
    }

    public record OrderCreatedEvent(Order order, List<OrderItem> items, List<ComboItem> comboItems) {}
    public record OrderEditedEvent(Order order, List<OrderItem> items, List<ComboItem> comboItems) {}
    public record OrderReadyEvent(
            Order order
    ) {}
    public record OrderPickedUpEvent(
            Order order
    ) {}
}


