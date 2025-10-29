package com.icpizza.backend.management.controller;

import com.icpizza.backend.management.dto.VendorTO;
import com.icpizza.backend.management.service.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VendorController {
    private final VendorService vendorService;

    @GetMapping("/get_all_vendors")
    public ResponseEntity<List<VendorTO>> getVendors(){
        return new ResponseEntity<>(vendorService.getAllVendors(), HttpStatus.OK);
    }
}
