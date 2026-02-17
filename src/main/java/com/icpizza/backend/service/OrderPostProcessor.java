package com.icpizza.backend.service;

import com.icpizza.backend.dto.order.PushOrderStatusUpdated;
import com.icpizza.backend.entity.*;
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
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPostProcessor {
    private final WhatsAppService wa;
    private final OrderEvents orderEvents;
    private final TikTokService tikTokService;
    private final BranchService branchService;
    private final MetaService metaService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW,readOnly = true)
    public void onOrderCreated(OrderCreatedEvent event) {
        String kitchenMsg = wa.buildKitchenMessage(event.items, event.comboItems);

        String tel = event.order.getCustomer() != null ? event.order.getCustomer().getTelephoneNo() : null;
        Branch branch = event.order.getBranch();
        int estimation = branchService.getEstimationByBranch(branch);
        String name = (event.order.getCustomer() != null ? event.order.getCustomer().getName() : null);
        if (tel != null && !tel.isBlank() && !"Unknown customer".equalsIgnoreCase(tel)) {
            String clientMsg = wa.buildOrderMessage(event.items, event.comboItems);
            wa.sendOrderConfirmation(tel, event.order,clientMsg);
            wa.sendEstimation(tel, estimation, event.order().getId());
            tikTokService.sendPlaceAnOrder(event.order.getCustomer().getTelephoneNo(), event.order.getAmountPaid());
            metaService.sendPurchaseEvent(event.order.getId(), event.order.getCustomer().getTelephoneNo());
        }

        wa.sendOrderToKitchenText2(event.order.getOrderNo(), kitchenMsg, tel, false, name);
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
                    && !orderReadyEvent.order.getCustomer().getTelephoneNo().isBlank() && !orderReadyEvent.order().getType().equals("Keeta")) {
                wa.sendOrderReady(orderReadyEvent.order().getCustomer().getTelephoneNo());
            }
            orderEvents.pushOrderStatusUpdate(new PushOrderStatusUpdated(orderReadyEvent.order.getId(),  orderReadyEvent.order.getStatus(), orderReadyEvent.order.getBranch().getId()));
        } catch (Exception ex) {
            log.error("[ready] WhatsApp send failed", ex);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPickedUp(OrderPickedUpEvent orderPickedUpEvent){
        try {
            String tel = orderPickedUpEvent.telephoneNo();

            if (tel != null && !tel.isBlank()) {
                wa.sendReadyMessage(tel,
                        orderPickedUpEvent.name(),
                        orderPickedUpEvent.id());
            }
            orderEvents.pushOrderStatusUpdate(new PushOrderStatusUpdated(orderPickedUpEvent.id(), orderPickedUpEvent.status(), orderPickedUpEvent.branchId()));
        } catch (Exception ex) {
            log.error("[ready] WhatsApp send failed", ex);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderInOven(PushOrderStatusUpdated newStatus){
        try {
            log.info("[OnOrderInOven] New Order Status: " + newStatus.toString());
            orderEvents.pushOrderStatusUpdate(newStatus);
        }
        catch (Exception ex) {
            log.error("[OVEN] Oven status send failed", ex);
        }
    }

    public record OrderCreatedEvent(Order order, List<OrderItem> items, List<ComboItem> comboItems) {}
    public record OrderEditedEvent(Order order, List<OrderItem> items, List<ComboItem> comboItems) {}
    public record OrderReadyEvent(
            Order order
    ) {}
    public record OrderPickedUpEvent(
            String telephoneNo,
            String name,
            Long id,
            String status,
            UUID branchId
    ) {}
}


