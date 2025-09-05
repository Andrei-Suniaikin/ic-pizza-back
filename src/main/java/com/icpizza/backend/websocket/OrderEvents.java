package com.icpizza.backend.websocket;

import com.icpizza.backend.cache.MenuSnapshot;
import com.icpizza.backend.entity.Order;
import com.icpizza.backend.entity.OrderItem;
import com.icpizza.backend.repository.OrderItemRepository;
import com.icpizza.backend.service.MenuService;
import com.icpizza.backend.websocket.dto.OrderAckTO;
import com.icpizza.backend.websocket.dto.OrderPushTO;
import com.icpizza.backend.websocket.mapper.WebsocketOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
@RequiredArgsConstructor
public class OrderEvents {
    private final SimpMessagingTemplate ws;
    private final WebsocketOrderMapper pushMapper;
    private final OrderItemRepository orderItemRepo;
    private final MenuService menuService;
    private final @Qualifier("orderAckScheduler") ThreadPoolTaskScheduler scheduler;

    private final Map<Long, ScheduledFuture<?>> pending = new ConcurrentHashMap<>();
    private final Map<Long, Integer> attempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 2;

    public void pushCreated(Order order, List<OrderItem> orderItems) {
        MenuSnapshot snap = menuService.getMenu();
        OrderPushTO payload = pushMapper.toPush(order, orderItems, snap);

        sendWithAck("/topic/orders", payload);
    }

    public void pushUpdated(Order order, List<OrderItem> orderItems) {
        MenuSnapshot snap = menuService.getMenu();
        OrderPushTO payload = pushMapper.toPush(order, orderItems, snap);

        sendWithAck("/topic/order-updates", payload);
    }

    public void pushReady(Long id){
        String dest = "/topic/order-ready";
        ws.convertAndSend(dest, id);

        attempts.putIfAbsent(id, 0);
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            int prev = attempts.getOrDefault(id, 0);
            if (prev >= MAX_ATTEMPTS) {
                pending.remove(id);
                attempts.remove(id);
                return;
            }
            attempts.put(id, prev + 1);
            ws.convertAndSend(dest, id);
            ScheduledFuture<?> again = scheduler.schedule(this::noop, new java.util.Date(System.currentTimeMillis() + 3000));
            pending.put(id, again);
        }, new java.util.Date(System.currentTimeMillis() + 3000));

        pending.put(id, future);
    }

    public void pushPaid(Long id){
        String dest = "/topic/order-paid";
        ws.convertAndSend(dest, id);

        attempts.putIfAbsent(id, 0);
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            int prev = attempts.getOrDefault(id, 0);
            if (prev >= MAX_ATTEMPTS) {
                pending.remove(id);
                attempts.remove(id);
                return;
            }
            attempts.put(id, prev + 1);
            ws.convertAndSend(dest, id);
            ScheduledFuture<?> again = scheduler.schedule(this::noop, new java.util.Date(System.currentTimeMillis() + 3000));
            pending.put(id, again);
        }, new java.util.Date(System.currentTimeMillis() + 3000));

        pending.put(id, future);
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
