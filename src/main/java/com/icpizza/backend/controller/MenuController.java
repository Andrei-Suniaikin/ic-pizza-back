package com.icpizza.backend.controller;

import com.icpizza.backend.dto.UpdateAvailabilityRequest;
import com.icpizza.backend.entity.Customer;
import com.icpizza.backend.repository.CustomerRepository;
import com.icpizza.backend.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MenuController {
    private final MenuService menuService;
    private final CustomerRepository customerRepository;

    @PutMapping("/update_availability")
    public ResponseEntity<String> updateAvailability(@RequestBody UpdateAvailabilityRequest request){
        int result = menuService.updateAvailability(request);
        return new ResponseEntity<>("Successsfully updated availability for "+result+" items"
                , HttpStatus.OK);
    }

    @GetMapping("/get_base_app_info")
    public HashMap<String, Object> getBaseAppInfo(@RequestParam(value = "userId", required = false)
                                                                  String userId,
                                                  @RequestParam(name = "branchId",  required = true)
                                                  UUID branchId){
        HashMap<String, Object> resp = menuService.getBaseAppInfo(branchId);


        if (userId != null && !userId.isBlank()) {
            Customer user = customerRepository.findByUserId(userId);
            if (user != null) {
                var userInfo = Map.of(
                        "name", user.getName() == null ? "Unknown User" : user.getName(),
                        "phone", user.getTelephoneNo()
                );
                resp.put("userInfo", userInfo);
            }
        }

        return resp;
    }
}
