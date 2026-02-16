package com.icpizza.backend.controller;

import com.icpizza.backend.dto.order.OrderPaymentTO;
import com.icpizza.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
