package com.jfl.appointment.dashboard.controller;

import com.jfl.appointment.dashboard.dto.UserResponse;
import com.jfl.appointment.dashboard.service.UserManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserManagementService userManagementService;


    @GetMapping()
    public UserResponse getUserDetail(@RequestParam("userName") String userName) {
        log.info("user id");
        return userManagementService.getUserDetail(userName);
    }
}
