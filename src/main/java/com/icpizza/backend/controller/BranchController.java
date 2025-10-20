package com.icpizza.backend.controller;

import com.icpizza.backend.dto.BranchTO;
import com.icpizza.backend.dto.UpdateWorkLoadLevelTO;
import com.icpizza.backend.enums.WorkLoadLevel;
import com.icpizza.backend.repository.BranchRepository;
import com.icpizza.backend.service.BranchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/get_workload_level")
    public ResponseEntity<WorkLoadLevel> getWorkloadLevel(@RequestParam("branchNumber") Integer branchNumber) {
        log.info("Get workload level for "+branchNumber);
        WorkLoadLevel level = branchService.getWorkLoadLevel(branchNumber);
        log.info("Get workload level for "+level);
        if (level == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(level, HttpStatus.OK);
    }

    @GetMapping("/get_branch_info")
    public ResponseEntity<BranchTO> getBranchInfo(@RequestParam("branchNumber") Integer branchNumber) {
        log.info("Get branch info for "+branchNumber);
        return new ResponseEntity<>(branchService.getBranchInfo(branchNumber), HttpStatus.OK);
    }
}
