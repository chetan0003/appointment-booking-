package com.jfl.appointment.dashboard.service;

import com.jfl.appointment.dashboard.dto.CreateAppointmentRequest;
import com.jfl.appointment.dashboard.dto.CreatePatientRequest;
import com.jfl.appointment.dashboard.dto.PatientResponseDto;
import com.jfl.appointment.dashboard.dto.UpdatePatientRequest;
import com.jfl.appointment.entity.*;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.repository.ClinicRepository;
import com.jfl.appointment.repository.PatientRepository;
import com.jfl.appointment.security.SecurityContextService;
import com.jfl.appointment.service.SubscriptionFeatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final ClinicRepository clinicRepository;
    private final SubscriptionFeatureService subscriptionFeatureService;
    private final SecurityContextService securityContextService;
    private final ClinicAccessService clinicAccessService;

    @Transactional
    public PatientResponseDto createPatient(
            Long clinicId,
            CreatePatientRequest request) {

        ClinicSubscription subscription =
                subscriptionFeatureService.getActiveSubscription(clinicId);
        //VALIDATE SUBSCRIPTION PLAN
        subscriptionFeatureService.validateFeature(
                clinicId,
                subscription,
                SubscriptionFeature.PATIENTS
        );

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

//        boolean exists =
//                patientRepository
//                        .existsByClinicIdAndWhatsappNumber(
//                                clinicId,
//                                request.whatsappNumber()
//                        );
//
//        if (exists) {
//            throw new IllegalArgumentException(
//                    "A patient with this WhatsApp number already exists."
//            );
//        }

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
        patient.setGender(request.gender());
        patient.setSource(PatientSource.DASHBOARD);
        patient.setProfileStatus(PatientProfileStatus.COMPLETE);
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


    @Transactional
    public PatientResponseDto updatePatient(
            Long clinicId,
            Long patientId,
            UpdatePatientRequest request) {

        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Clinic not found: " + clinicId
                        ));

        Patient patient = patientRepository
                .findById(patientId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Patient not found: " + patientId
                        ));

        // Important: ensure patient belongs to this clinic
        if (!patient.getClinic().getId().equals(clinic.getId())) {
            throw new IllegalArgumentException(
                    "Patient does not belong to clinic: " + clinicId
            );
        }

        patient.setName(request.name());
        patient.setWhatsappNumber(request.whatsappNumber());
        patient.setEmail(request.email());
        patient.setDateOfBirth(request.dateOfBirth());
        patient.setGender(request.gender());
        //patient.setSource(PatientSource.DASHBOARD);
        patient.setProfileStatus(
                isProfileComplete(patient)
                        ? PatientProfileStatus.COMPLETE
                        : PatientProfileStatus.INCOMPLETE
        );

        return toDto(patientRepository.save(patient));
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

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "name")
        );

        Collection<? extends GrantedAuthority> authorities = securityContextService.getCurrentUser().getAuthorities();
        RoleName currentUserRole = authorities.stream().map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5))
                .map(authority -> RoleName.valueOf(authority))
                .findFirst()
                .orElse(null);

        Page<Patient> patients = null;

        switch (currentUserRole) {

            case SUPER_ADMIN, CLINIC_ADMIN, STAFF -> patients =
                     patientRepository.findByClinicId(
                            clinicId,
                            pageable
                    );

            case DOCTOR -> {

                ClinicUser clinicUser =
                        clinicAccessService
                                .getClinicUser(clinicId);

                Long doctorId =
                        clinicUser.getDoctor().getId();

                patients = patientRepository.findPatientsByDoctor(
                        clinicId,
                        doctorId,
                        pageable
                );
            }

            default -> throw new SecurityException(
                    "You are not authorized to view patients."
            );
        }

        return patients.map(this::toDto);
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
        return new PatientResponseDto(
                patient.getId(),
                patient.getName(),
                patient.getWhatsappNumber(),
                patient.getEmail(),
                patient.getDateOfBirth(),
                patient.getClinic().getId(),
                patient.getGender() != null ? patient.getGender().name() : null,
                patient.getSource() != null ? patient.getSource().name() : null,
                patient.getProfileStatus() != null ? patient.getProfileStatus().name() : null
        );
    }

    private boolean isProfileComplete(Patient patient) {
        return patient.getName() != null
                && !patient.getName().isBlank()
                && patient.getWhatsappNumber() != null
                && !patient.getWhatsappNumber().isBlank()
                && patient.getEmail() != null
                && !patient.getEmail().isBlank()
                && patient.getDateOfBirth() != null
                && patient.getGender() != null;
    }

    private Patient createWhatsAppPatient(
            CreateAppointmentRequest request,
            Clinic clinic) {

        Patient patient = new Patient();

        patient.setClinic(clinic);
        patient.setName(request.patientName());
        patient.setWhatsappNumber(request.whatsappNumber());
        patient.setSource(PatientSource.WHATSAPP);
        patient.setProfileStatus(PatientProfileStatus.INCOMPLETE);

        return patientRepository.save(patient);
    }
}
