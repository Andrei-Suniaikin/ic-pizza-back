package com.icpizza.backend.controller;

import com.icpizza.backend.dto.order.CheckCustomerResponse;
import com.icpizza.backend.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;

    @GetMapping("/check_customer")
    public ResponseEntity<CheckCustomerResponse> checkCustomer(@RequestParam(name = "tel") String telephoneNo){
        return new ResponseEntity<>(customerService.checkCustomer(telephoneNo), HttpStatus.OK);
    }
}
