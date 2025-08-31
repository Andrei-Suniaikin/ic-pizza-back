package com.icpizza.backend.controller;

import com.icpizza.backend.dto.*;
import com.icpizza.backend.entity.Order;
import com.icpizza.backend.service.OrderService;
import com.icpizza.backend.websocket.OrderEvents;
import com.icpizza.backend.websocket.dto.OrderAckTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final OrderEvents orderEvents;

    @PostMapping("/status_update")
    public ResponseEntity<Order> orderStatusUpdate(@RequestBody OrderStatusUpdateTO newStatus){
        Order order = orderService.updateOrderStatus(newStatus);
        return new ResponseEntity<>(order, HttpStatus.OK);
    }

    @PostMapping("/create_order")
    public ResponseEntity<CreateOrderTO> createOrder(@RequestBody CreateOrderTO orderTO){
        CreateOrderTO order = orderService.createWebsiteOrder(orderTO);
        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }

    @PutMapping("/edit_order")
    public ResponseEntity<Map<String, String>> editOrder(@RequestBody EditOrderTO editOrderTO){
        orderService.editOrder(editOrderTO);
        return new ResponseEntity<>(Map.of("Response", "Order "+editOrderTO.orderNo()+" successfully edited")
                ,HttpStatus.OK);
    }

    @GetMapping("/get_all_active_orders")
    public ResponseEntity<Map<String, List<ActiveOrdersTO>>> getAllActiveOrders(){
        Map<String,List<ActiveOrdersTO>> activeOrders = orderService.getAllActiveOrders();
        return new ResponseEntity<>(activeOrders, HttpStatus.OK);
    }

    @GetMapping("/get_history")
    public ResponseEntity<Map<String, List<OrderHistoryTO>>> getHistory(){
        return new ResponseEntity<>(orderService.getHistory(), HttpStatus.OK);
    }

    @PutMapping("/update_payment_type")
    public ResponseEntity<Map<String, String>> updatePaymentType(@RequestBody UpdatePaymentTypeTO updatePaymentType){
        orderService.updatePaymentType(updatePaymentType);
        return new ResponseEntity<>(Map.of("Response", "Payment type successfully updated for order with ID: "
                + updatePaymentType.id()),
                HttpStatus.OK);
    }

    @PutMapping("/ready_action")
    public ResponseEntity<Map<String, String>> markOrderReady(@RequestBody MarkOrderReadyTO markOrderReadyTO){
        log.info(String.valueOf(markOrderReadyTO));

        orderService.markOrderReady(markOrderReadyTO);

        return new ResponseEntity<>(Map.of("Response" , "Order with ID: "
                +markOrderReadyTO.id()+" marked as: Ready") ,
                HttpStatus.OK);
    }

    @MessageMapping("/orders/ack")
    public void ack(OrderAckTO ack) {
        orderEvents.handleAck(ack);
    }
}
