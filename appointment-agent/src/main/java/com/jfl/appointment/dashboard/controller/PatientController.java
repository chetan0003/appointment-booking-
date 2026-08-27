package com.jfl.appointment.dashboard.controller;

import com.jfl.appointment.dashboard.dto.PatientResponseDto;
import com.jfl.appointment.dashboard.service.PatientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/dashboard/patient")
@RequiredArgsConstructor
public class PatientController {


    private final PatientService patientService;

    @GetMapping
    public ResponseEntity<List<PatientResponseDto>> getAllPatient(@RequestParam("clinicId") Long clinicId) {
        List<PatientResponseDto> allClinic = patientService.getAllPatient(clinicId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(allClinic);
    }
}
