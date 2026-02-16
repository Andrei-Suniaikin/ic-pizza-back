package com.icpizza.backend.controller;

import com.icpizza.backend.dto.branch.*;
import com.icpizza.backend.management.dto.VatResponse;
import com.icpizza.backend.repository.EventRepository;
import com.icpizza.backend.service.BranchService;
import com.icpizza.backend.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/branch")
@RequiredArgsConstructor
public class BranchController {
    private final BranchService branchService;
    private final EventService eventService;

    @PutMapping("/update_workload_level")
    public ResponseEntity<String> updateWorkloadLevel(@RequestBody UpdateWorkLoadLevelTO updateWorkLoadLevelTO) {
        boolean answer = branchService.setWorkloadLevel(updateWorkLoadLevelTO);
        if(answer) return new ResponseEntity<>("Successfully updated workload level for "+answer, HttpStatus.OK);

        return new ResponseEntity<>("Failed to update workload level for "+answer, HttpStatus.BAD_REQUEST);
    }

    @GetMapping("/get_branch_info")
    public ResponseEntity<BranchTO> getBranchInfo(@RequestParam("branchId") UUID branchId) {
        log.info("Get branch info for "+branchId);
        return new ResponseEntity<>(branchService.getBranchInfo(branchId), HttpStatus.OK);
    }

    @GetMapping("/get_admin_base_info")
    public ResponseEntity<BaseAdminResponse> getAdminBaseInfo(@RequestParam("branchId") UUID branchId) {
        return new ResponseEntity<>(branchService.getAdminBaseInfo(branchId), HttpStatus.OK);
    }

    @PostMapping("/send_shift_event")
    public ResponseEntity<ShiftEventResponse> sendShiftEvent(@RequestBody ShiftEventRequest request){
        return new ResponseEntity<>(branchService.createEvent(request), HttpStatus.OK);
    }

    @GetMapping("/get_vat_stats")
    public ResponseEntity<VatResponse> getVatStats(@RequestParam("branchId") UUID branchid,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return new ResponseEntity<>(branchService.getVatStats(branchid, fromDate, toDate), HttpStatus.OK);
    }

    @GetMapping("/fetch_branches")
    public ResponseEntity<List<BranchTO>> fetchBranches() {
        return new ResponseEntity<>(branchService.getAllBranches(), HttpStatus.OK);
    }

    @PostMapping("/cash_update")
    public ResponseEntity<BranchBalanceResponse> cashUpdate(@RequestBody CashUpdateRequest request){
        log.info("Cash update request for "+request);
        return new ResponseEntity<>(branchService.cashUpdate(request), HttpStatus.OK);
    }

    @GetMapping("/get_branch_balance")
    public ResponseEntity<BranchBalanceResponse> getBranchBalance(@RequestParam("branchId") UUID branchId) {
        return new ResponseEntity<>(branchService.getBranchBalance(branchId), HttpStatus.OK);
    }

    @GetMapping("/get_transactions")
    public ResponseEntity<List<CashRegisterEventTO>> getTransactions(@RequestParam("branchId") UUID branchId) {
        return new ResponseEntity<>(eventService.getEvents(branchId), HttpStatus.OK);
    }
}
