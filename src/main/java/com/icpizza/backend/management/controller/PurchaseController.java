package com.icpizza.backend.management.controller;

import com.icpizza.backend.management.dto.purchase.BasePurchaseResponse;
import com.icpizza.backend.management.dto.purchase.CreatePurchaseTO;
import com.icpizza.backend.management.dto.purchase.EditPurchaseTO;
import com.icpizza.backend.management.dto.purchase.PurchaseTO;
import com.icpizza.backend.management.service.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PurchaseController {
    private final PurchaseService purchaseService;

    @GetMapping("/get_purchase_reports")
    public ResponseEntity<List<BasePurchaseResponse>> getPurchaseReports() {
        List<BasePurchaseResponse> purchaseReports = purchaseService.getPurchaseReports();
        return new ResponseEntity<>(purchaseReports, HttpStatus.OK);
    }

    @PostMapping("/create_purchase_report")
    public ResponseEntity<BasePurchaseResponse> createPurchaseReport(@RequestBody CreatePurchaseTO purchaseTO) {
        BasePurchaseResponse newReport = purchaseService.createPurchaseReport(purchaseTO);
        return new ResponseEntity<>(newReport, HttpStatus.OK);
    }

    @GetMapping("/get_purchase_report")
    public ResponseEntity<PurchaseTO> getPurchaseReport(@RequestParam Long id){
        return new ResponseEntity<>(purchaseService.getPurchaseReport(id), HttpStatus.OK);
    }

    @PutMapping("/edit_purchase_report")
    public ResponseEntity<BasePurchaseResponse> editPurchaseReport(@RequestBody EditPurchaseTO editpurchaseTO) {
        return new ResponseEntity<>(purchaseService.editPurchaseReport(editpurchaseTO), HttpStatus.OK);
    }
}
