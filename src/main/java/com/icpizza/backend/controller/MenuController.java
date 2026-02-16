package com.icpizza.backend.controller;

import com.icpizza.backend.dto.menu.MenuResponse;
import com.icpizza.backend.dto.menu.UpdateAvailabilityRequest;
import com.icpizza.backend.dto.order.UserInfoDTO;
import com.icpizza.backend.entity.Customer;
import com.icpizza.backend.repository.CustomerRepository;
import com.icpizza.backend.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public MenuResponse getBaseAppInfo(@RequestParam(value = "userId", required = false)
                                                                  String userId,
                                                  @RequestParam(name = "branchId",  required = true)
                                                  UUID branchId){
        MenuResponse resp = menuService.getBaseAppInfo(branchId);

        if (userId != null && !userId.isBlank()) {
            Customer user = customerRepository.findByUserId(userId);
            if (user != null) {
                UserInfoDTO userInfo = UserInfoDTO.builder()
                        .name(user.getName() == null? "Unknown user": user.getName())
                        .telephoneNo(user.getTelephoneNo())
                        .build();

                return new MenuResponse(
                        resp.mainMenu(),
                        resp.extras(),
                        resp.toppings(),
                        userInfo
                );
            }
        }
        return resp;
    }
}
