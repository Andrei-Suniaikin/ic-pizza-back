package com.icpizza.backend.controller;

import com.icpizza.backend.dto.OrderPaymentTO;
import com.icpizza.backend.entity.Order;
import com.icpizza.backend.repository.OrderRepository;
import com.icpizza.backend.service.OrderService;
import com.icpizza.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/order_payment")
    public ResponseEntity<Map<String, Object>> orderPayment(@RequestBody OrderPaymentTO orderPaymentTO){
        paymentService.orderPayment(orderPaymentTO);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
