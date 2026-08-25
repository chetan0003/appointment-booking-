package com.jfl.appointment.dashboard.dto;


import java.util.List;

public record LoginResponse(

        String token,

        Long userId,

        String username,

        List<String> roles

) {}
