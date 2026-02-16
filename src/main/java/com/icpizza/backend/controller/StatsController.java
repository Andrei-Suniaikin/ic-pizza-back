package com.icpizza.backend.controller;

import com.icpizza.backend.dto.stats.StatsResponse;
import com.icpizza.backend.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class StatsController {
    private final StatsService statsService;

    @GetMapping("/get_statistics")
    public ResponseEntity<StatsResponse> getStatistics(@RequestParam("start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                       @RequestParam("finish_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate finishDate,
                                                       @RequestParam(value = "certain_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate certainDate){
        return new ResponseEntity<>(statsService.getStatistics(startDate, finishDate, certainDate),
                                                                HttpStatus.OK);
    }
}
