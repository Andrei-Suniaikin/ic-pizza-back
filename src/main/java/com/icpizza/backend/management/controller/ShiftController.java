package com.icpizza.backend.management.controller;

import com.icpizza.backend.management.dto.shift.BaseShiftResponse;
import com.icpizza.backend.management.dto.shift.CreateShiftReportTO;
import com.icpizza.backend.management.dto.shift.EditShiftReportTO;
import com.icpizza.backend.management.dto.shift.ShiftReportTO;
import com.icpizza.backend.management.service.ShiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ShiftController {
    private final ShiftService shiftService;

    @GetMapping("/get_all_shift_reports")
    public ResponseEntity<List<BaseShiftResponse>> fetchAllShiftReports(){
        return new ResponseEntity<>(shiftService.getAllShiftReports(), HttpStatus.OK);
    }

    @PostMapping("/create_shift_report")
    public ResponseEntity<BaseShiftResponse> createShiftReport(@RequestBody CreateShiftReportTO createShiftReportTO){
        return new ResponseEntity<>(shiftService.createShiftReport(createShiftReportTO), HttpStatus.OK);
    }

    @PutMapping("/edit_shift_report")
    public ResponseEntity<BaseShiftResponse> editShiftReport(@RequestBody EditShiftReportTO editShiftReportTO){
        return new ResponseEntity<>(shiftService.editShiftReport(editShiftReportTO), HttpStatus.OK);
    }

    @GetMapping("/get_shift_report")
    public ResponseEntity<ShiftReportTO> getShiftReport(@RequestParam(name = "id") Long id){
        return new ResponseEntity<>(shiftService.findReportById(id), HttpStatus.OK);
    }
}
