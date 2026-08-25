package com.jfl.appointment.n8n.controller;

import com.jfl.appointment.n8n.dto.AppointmentResponse;
import com.jfl.appointment.n8n.dto.CreateAppointmentRequest;
import com.jfl.appointment.n8n.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/n8n/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse createAppointment(@Valid @RequestBody CreateAppointmentRequest request) {
        return appointmentService.createAppointment(request);
    }
}
