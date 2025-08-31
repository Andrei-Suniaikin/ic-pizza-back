package com.icpizza.backend.controller;

import com.icpizza.backend.dto.ShiftEventRequest;
import com.icpizza.backend.dto.ShiftEventResponse;
import com.icpizza.backend.enums.EventType;
import com.icpizza.backend.service.ShiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ShiftController {
    private final ShiftService shiftService;

    @PostMapping("/send_shift_event")
    public ResponseEntity<ShiftEventResponse> sendShiftEvent(@RequestBody ShiftEventRequest request){
        return new ResponseEntity<>(shiftService.createEvent(request), HttpStatus.OK);
    }

    @GetMapping("/get_last_stage")
    public ResponseEntity<Map<String, EventType>> getLastStage(@RequestParam String branchId){
        return new ResponseEntity<>(shiftService.getLastStage(branchId), HttpStatus.OK);
    }
}
