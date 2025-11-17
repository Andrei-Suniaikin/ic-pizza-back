package com.icpizza.backend.management.controller;

import com.icpizza.backend.management.dto.ConsumptionReportTO;
import com.icpizza.backend.management.service.ConsumptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ConsumptionController {
    private final ConsumptionService consumptionService;

    @GetMapping("/get_consumption_report")
    public ResponseEntity<ConsumptionReportTO> getConsumptionReport(){
        return new ResponseEntity<>(consumptionService.getLatestReport(), HttpStatus.OK);
    }
}
