package com.jfl.appointment.dashboard.service;

import com.jfl.appointment.dashboard.dto.PatientResponseDto;
import com.jfl.appointment.entity.Patient;
import com.jfl.appointment.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;

    public List<PatientResponseDto> getAllPatient(Long clinicId) {
        if(!StringUtils.isEmpty(clinicId))
            return patientRepository.findByClinicId(clinicId).stream().map(this::toDto).toList();
       return patientRepository.findAll().stream().map(this::toDto).toList();
    }

    private PatientResponseDto toDto(Patient patient) {
        return new PatientResponseDto(patient.getId(),patient.getName(),patient.getWhatsappNumber(),patient.getClinic().getId());
    }

}
