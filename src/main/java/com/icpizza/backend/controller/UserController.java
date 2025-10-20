package com.icpizza.backend.controller;

import com.icpizza.backend.dto.UserTO;
import com.icpizza.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/get_user")
    public ResponseEntity<UserTO> getUser(@RequestParam(name = "userId") Long userId){

        UserTO userTO = userService.getUser(userId);
        return ResponseEntity.ok(userTO);
    }
}
