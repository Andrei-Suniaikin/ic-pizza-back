package com.icpizza.backend.keeta.controller;

import com.icpizza.backend.keeta.dto.CreateKeetaOrderTO;
import com.icpizza.backend.keeta.service.KeetaOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/keeta")
@RequiredArgsConstructor
public class KeetaOrderController {
    private final KeetaOrderService keetaOrderService;

    @PostMapping("/create_order")
    public ResponseEntity<String> createOrder(@RequestBody CreateKeetaOrderTO createKeetaOrderTO) {
        return new ResponseEntity<>(keetaOrderService.createKeetaOrder(createKeetaOrderTO), HttpStatus.OK);
    }
}
