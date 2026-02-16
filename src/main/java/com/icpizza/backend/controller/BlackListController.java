package com.icpizza.backend.controller;

import com.icpizza.backend.dto.blacklist.BlackListRequest;
import com.icpizza.backend.dto.blacklist.BlackListResponse;
import com.icpizza.backend.service.BlackListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/blacklist")
public class BlackListController {
    private final BlackListService blackListService;

    @PostMapping("/add")
    public ResponseEntity<BlackListResponse> addToBlackList(@RequestBody BlackListRequest blackListRequest) {
        return new ResponseEntity<>(blackListService.addToBlackList(blackListRequest), HttpStatus.OK);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteFromBlackList(@RequestBody BlackListRequest blackListRequest) {
        blackListService.deleteFromBlackList(blackListRequest);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/get_all")
    public ResponseEntity<List<BlackListResponse>> getAll(){
        return new ResponseEntity<>(blackListService.getAll(), HttpStatus.OK);
    }
}
