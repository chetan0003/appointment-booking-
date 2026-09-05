package com.jfl.appointment.dashboard.service;

import com.jfl.appointment.dashboard.dto.ClinicResponse;
import com.jfl.appointment.dashboard.dto.CreateClinicRequest;
import com.jfl.appointment.entity.Clinic;
import com.jfl.appointment.repository.ClinicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClinicService {

    private final ClinicRepository clinicRepository;

    @Transactional
    public ClinicResponse createClinic(CreateClinicRequest request) {

        Clinic clinic = new Clinic();

        clinic.setName(request.name());
        clinic.setWhatsappNumber(request.whatsappNumber());
        clinic.setTimezone(request.timezone());
        clinic.setActive(true);

        Clinic savedClinic = clinicRepository.save(clinic);

        return new ClinicResponse(
                savedClinic.getId(),
                savedClinic.getName(),
                savedClinic.getWhatsappNumber(),
                savedClinic.getTimezone(),
                savedClinic.isActive(),
                savedClinic.getCreatedAt()
        );
    }

    public List<ClinicResponse> getAllClinic() {
        return clinicRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public ClinicResponse toDto(Clinic savedClinic) {
        return new ClinicResponse(
                savedClinic.getId(),
                savedClinic.getName(),
                savedClinic.getWhatsappNumber(),
                savedClinic.getTimezone(),
                savedClinic.isActive(),
                savedClinic.getCreatedAt());
    }
}