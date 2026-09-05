package com.jfl.appointment.dashboard.service;

import com.jfl.appointment.dashboard.dto.CreatePatientRequest;
import com.jfl.appointment.dashboard.dto.PatientResponseDto;
import com.jfl.appointment.entity.Clinic;
import com.jfl.appointment.entity.Patient;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.repository.ClinicRepository;
import com.jfl.appointment.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final ClinicRepository clinicRepository;

    @Transactional
    public PatientResponseDto createPatient(
            Long clinicId,
            CreatePatientRequest request) {

        // =====================================================
        // 1. Validate clinic
        // =====================================================

        Clinic clinic =
                clinicRepository
                        .findById(clinicId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Clinic not found: " + clinicId
                                )
                        );

        // =====================================================
        // 2. Validate duplicate WhatsApp number
        // =====================================================

        boolean exists =
                patientRepository
                        .existsByClinicIdAndWhatsappNumber(
                                clinicId,
                                request.whatsappNumber()
                        );

        if (exists) {
            throw new IllegalArgumentException(
                    "A patient with this WhatsApp number already exists."
            );
        }

        // =====================================================
        // 3. Validate DOB
        // =====================================================

        if (request.dateOfBirth() != null
                && request.dateOfBirth().isAfter(LocalDate.now())) {

            throw new IllegalArgumentException(
                    "Date of birth cannot be in the future."
            );
        }

        // =====================================================
        // 4. Create patient
        // =====================================================

        Patient patient = new Patient();

        patient.setClinic(clinic);
        patient.setName(request.name().trim());
        patient.setWhatsappNumber(
                request.whatsappNumber().trim()
        );
        patient.setEmail(
                request.email() != null
                        ? request.email().trim()
                        : null
        );
        patient.setDateOfBirth(
                request.dateOfBirth()
        );
        //patient.setActive(true);

        // =====================================================
        // 5. Save
        // =====================================================

        Patient savedPatient =
                patientRepository.save(patient);

        log.info(
                "Patient created successfully. patientId={}, clinicId={}",
                savedPatient.getId(),
                clinicId
        );

        // =====================================================
        // 6. Response
        // =====================================================

        return toDto(savedPatient);
    }



    @Transactional(readOnly = true)
    public Page<PatientResponseDto> getAllPatient(
            Long clinicId,
            int page,
            int size) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page must be greater than or equal to 0."
            );
        }

        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100."
            );
        }

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Direction.ASC,
                                "name"
                        )
                );

        return patientRepository
                .findByClinicId(
                        clinicId,
                        pageable
                )
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<PatientResponseDto> searchPatients(
            Long clinicId,
            String query) {

        // ---------------------------------------------
        // Validate clinic
        // ---------------------------------------------

        if (!clinicRepository.existsById(clinicId)) {
            throw new NotFoundException(
                    "Clinic not found: " + clinicId
            );
        }

        // ---------------------------------------------
        // Validate search query
        // ---------------------------------------------

        if (query == null || query.trim().length() < 2) {
            throw new IllegalArgumentException(
                    "Search query must contain at least 2 characters."
            );
        }

        String searchQuery = query.trim();

        return patientRepository
                .searchPatients(
                        clinicId,
                        searchQuery
                )
                .stream()
                .map(this::toDto)
                .toList();
    }

    private PatientResponseDto toDto(Patient patient) {
        return new PatientResponseDto(patient.getId(),patient.getName(),patient.getWhatsappNumber(),patient.getEmail(),patient.getDateOfBirth(),patient.getClinic().getId());
    }

}
