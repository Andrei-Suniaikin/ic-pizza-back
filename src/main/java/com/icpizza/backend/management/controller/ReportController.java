package com.icpizza.backend.management.controller;

import com.icpizza.backend.management.dto.*;
import com.icpizza.backend.management.entity.Report;
import com.icpizza.backend.management.repository.ReportRepository;
import com.icpizza.backend.management.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/base_management")
    public ResponseEntity<List<BaseManagementResponse>> getBaseManagementReports(@RequestParam(name = "branchNo") Integer branchNo) {
        log.info("[REPORTS] getting all reports for branch {}", branchNo);
        List<BaseManagementResponse> reports = reportService.getAllReportsByBranch(branchNo);
        return ResponseEntity.ok(reports);
    }

    @PostMapping("/create_report")
    public ResponseEntity<BaseManagementResponse> createReport(@RequestBody CreateReportTO createReportTO){
        log.info("[REPORTS] creating report {}", createReportTO);
        return new ResponseEntity<>(reportService.createReport(createReportTO), HttpStatus.OK);
    }

    @PutMapping("/report_edit")
    public ResponseEntity<BaseManagementResponse> editReport(@RequestBody EditReportTO editReportTO){
        log.info("[REPORTS] editing report {}", editReportTO);
        return new ResponseEntity<>(reportService.editReport(editReportTO), HttpStatus.OK);
    }

    @GetMapping("/get_report")
    public ResponseEntity<ReportTO> getReport(@RequestParam(name = "reportId") Long id){
        log.info("[REPORTS] getting report {}", id);
        ReportTO reportTO = reportService.getReport(id);
        return new ResponseEntity<>(reportTO, HttpStatus.OK);
    }

    @GetMapping("/fetch_products")
    public ResponseEntity<List<ProductTO>> fetchProducts(){
        log.info("[REPORTS] fetching products");
        List<ProductTO> products = reportService.fetchProducts();
        return ResponseEntity.ok(products);
    }
}
