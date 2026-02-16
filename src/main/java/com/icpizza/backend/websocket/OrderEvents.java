package com.icpizza.backend.websocket;

import com.icpizza.backend.cache.MenuSnapshot;
import com.icpizza.backend.dto.order.PushOrderStatusUpdated;
import com.icpizza.backend.entity.Order;
import com.icpizza.backend.entity.OrderItem;
import com.icpizza.backend.service.MenuService;
import com.icpizza.backend.websocket.dto.OrderAckTO;
import com.icpizza.backend.websocket.dto.OrderPushTO;
import com.icpizza.backend.websocket.mapper.WebsocketOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEvents {
    private final SimpMessagingTemplate ws;
    private final WebsocketOrderMapper pushMapper;
    private final MenuService menuService;
    private final @Qualifier("orderAckScheduler") ThreadPoolTaskScheduler scheduler;

    private final Map<Long, ScheduledFuture<?>> pending = new ConcurrentHashMap<>();
    private final Map<Long, Integer> attempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 2;

    public void pushCreated(Order order, List<OrderItem> orderItems) {
        log.info("[PUSH CREATED] orderId={} successfully created",  order.getId());
        MenuSnapshot snap = menuService.getMenu(order.getBranch().getId());
        OrderPushTO payload = pushMapper.toPush(order, orderItems, snap);

        sendWithAck("/topic/" +order.getBranch().getId() + "/orders", payload);
    }

    public void pushUpdated(Order order, List<OrderItem> orderItems) {
        MenuSnapshot snap = menuService.getMenu(order.getBranch().getId());
        OrderPushTO payload = pushMapper.toPush(order, orderItems, snap);

        sendWithAck("/topic/" + order.getBranch().getId() + "/order-updates", payload);
    }

    public void pushPaid(Long orderId, UUID branchId){
        String dest = "/topic/" + branchId + "/order-paid";
        ws.convertAndSend(dest, orderId);

        attempts.putIfAbsent(orderId, 0);
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            int prev = attempts.getOrDefault(orderId, 0);
            if (prev >= MAX_ATTEMPTS) {
                pending.remove(orderId);
                attempts.remove(orderId);
                return;
            }
            attempts.put(orderId, prev + 1);
            ws.convertAndSend(dest, orderId);
            ScheduledFuture<?> again = scheduler.schedule(this::noop, new java.util.Date(System.currentTimeMillis() + 3000));
            pending.put(orderId, again);
        }, new java.util.Date(System.currentTimeMillis() + 3000));

        pending.put(orderId, future);
    }

    public void pushAccepted(Long orderId, UUID branchId){
        String dest = "/topic/" + branchId + "/order-accepted";
        ws.convertAndSend(dest, orderId);

        attempts.putIfAbsent(orderId, 0);
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            int prev = attempts.getOrDefault(orderId, 0);
            if (prev >= MAX_ATTEMPTS) {
                pending.remove(orderId);
                attempts.remove(orderId);
                return;
            }
            attempts.put(orderId, prev + 1);
            ws.convertAndSend(dest, orderId);
            ScheduledFuture<?> again = scheduler.schedule(this::noop, new java.util.Date(System.currentTimeMillis() + 3000));
            pending.put(orderId, again);
        }, new java.util.Date(System.currentTimeMillis() + 3000));

        pending.put(orderId, future);
    }


    public void pushDeleted(Long orderId, UUID branchId) {
        String dest = "/topic/" + branchId + "/order-cancelled";
        ws.convertAndSend(dest, orderId);

        attempts.putIfAbsent(orderId, 0);
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            int prev = attempts.getOrDefault(orderId, 0);
            if (prev >= MAX_ATTEMPTS) {
                pending.remove(orderId);
                attempts.remove(orderId);
                return;
            }
            attempts.put(orderId, prev + 1);
            ws.convertAndSend(dest, orderId);
            ScheduledFuture<?> again = scheduler.schedule(this::noop, new java.util.Date(System.currentTimeMillis() + 3000));
            pending.put(orderId, again);
        }, new java.util.Date(System.currentTimeMillis() + 3000));

        pending.put(orderId, future);
    }

    public void pushOrderStatusUpdate(PushOrderStatusUpdated pushOrderStatusUpdated){
        log.info("[PUSH_ORDER_STATUS_UPDATE_EVENT] request: {}", pushOrderStatusUpdated);
        String dest = "/topic/"+ pushOrderStatusUpdated.branchId() + "/order-status-updated";
        ws.convertAndSend(dest, pushOrderStatusUpdated);

        attempts.putIfAbsent(pushOrderStatusUpdated.id(), 0);
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            int prev = attempts.getOrDefault(pushOrderStatusUpdated.id(), 0);
            if (prev >= MAX_ATTEMPTS) {
                pending.remove(pushOrderStatusUpdated.id());
                attempts.remove(pushOrderStatusUpdated.id());
                return;
            }
            attempts.put(pushOrderStatusUpdated.id(), prev + 1);
            ws.convertAndSend(dest, pushOrderStatusUpdated);
            ScheduledFuture<?> again = scheduler.schedule(this::noop, new java.util.Date(System.currentTimeMillis() + 3000));
            pending.put(pushOrderStatusUpdated.id(), again);
        }, new java.util.Date(System.currentTimeMillis() + 3000));

        pending.put(pushOrderStatusUpdated.id(), future);
    }

    public void handleAck(OrderAckTO ack) {
        if (ack == null || ack.orderId() == null) return;
        ScheduledFuture<?> f = pending.remove(ack.orderId());
        attempts.remove(ack.orderId());
        if (f != null) f.cancel(false);
    }

    private void sendWithAck(String dest, OrderPushTO payload) {
        Long id = payload.orderId();
        ws.convertAndSend(dest, payload);

        attempts.putIfAbsent(id, 0);
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            int prev = attempts.getOrDefault(id, 0);
            if (prev >= MAX_ATTEMPTS) {
                pending.remove(id);
                attempts.remove(id);
                return;
            }
            attempts.put(id, prev + 1);
            ws.convertAndSend(dest, payload);
            ScheduledFuture<?> again = scheduler.schedule(this::noop, new java.util.Date(System.currentTimeMillis() + 3000));
            pending.put(id, again);
        }, new java.util.Date(System.currentTimeMillis() + 3000));

        pending.put(id, future);
    }
    private void noop() {}
}
