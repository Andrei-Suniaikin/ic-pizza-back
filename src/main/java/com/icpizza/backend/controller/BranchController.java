package com.icpizza.backend.controller;

import com.icpizza.backend.dto.*;
import com.icpizza.backend.management.dto.VatResponse;
import com.icpizza.backend.service.BranchService;
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
    public ResponseEntity<VatResponse> getVatStats(@RequestParam("branchId") Integer branchNo,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return new ResponseEntity<>(branchService.getVatStats(branchNo, fromDate, toDate), HttpStatus.OK);
    }

    @GetMapping("/fetch_branches")
    public ResponseEntity<List<BranchTO>> fetchBranches() {
        return new ResponseEntity<>(branchService.getAllBranches(), HttpStatus.OK);
    }
}
