package com.jfl.appointment.dashboard.service;

import com.jfl.appointment.dashboard.dto.ClinicResponse;
import com.jfl.appointment.dashboard.dto.CreateClinicRequest;
import com.jfl.appointment.entity.Clinic;
import com.jfl.appointment.exception.NotFoundException;
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
        clinic.setAddressLine1(request.addressLine1());
        clinic.setAddressLine2(request.addressLine2());
        clinic.setCountryCode("IN");
        clinic.setState(request.state());
        clinic.setCity(request.city());
        clinic.setPostalCode(request.postalCode());
        clinic.setLatitude(request.latitude());
        clinic.setLongitude(request.longitude());

        Clinic savedClinic = clinicRepository.save(clinic);

        return new ClinicResponse(
                savedClinic.getId(),
                savedClinic.getName(),
                savedClinic.getWhatsappNumber(),
                savedClinic.getTimezone(),
                savedClinic.isActive(),
                savedClinic.getCountryCode(),
                savedClinic.getState(),
                savedClinic.getCity(),
                savedClinic.getPostalCode(),
                savedClinic.getAddressLine1(),
                savedClinic.getAddressLine2(),
                savedClinic.getLatitude(),
                savedClinic.getLongitude(),
                savedClinic.getCreatedAt()
        );
    }

    @Transactional
    public ClinicResponse updateClinic(Long id,CreateClinicRequest request) {


        Clinic clinic = clinicRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Clinic not found with id: " + id));

        clinic.setName(request.name());
        clinic.setWhatsappNumber(request.whatsappNumber());
        clinic.setTimezone(request.timezone());

        clinic.setAddressLine1(request.addressLine1());
        clinic.setAddressLine2(request.addressLine2());
        clinic.setCountryCode("IN");
        clinic.setState(request.state());
        clinic.setCity(request.city());
        clinic.setPostalCode(request.postalCode());
        clinic.setLatitude(request.latitude());
        clinic.setLongitude(request.longitude());

        clinic.setActive(true);

        Clinic savedClinic = clinicRepository.save(clinic);

        return new ClinicResponse(
                savedClinic.getId(),
                savedClinic.getName(),
                savedClinic.getWhatsappNumber(),
                savedClinic.getTimezone(),
                savedClinic.isActive(),
                savedClinic.getCountryCode(),
                savedClinic.getState(),
                savedClinic.getCity(),
                savedClinic.getPostalCode(),
                savedClinic.getAddressLine1(),
                savedClinic.getAddressLine2(),
                savedClinic.getLatitude(),
                savedClinic.getLongitude(),
                savedClinic.getCreatedAt()
        );
    }


    public List<ClinicResponse> getAllClinic() {
        return clinicRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public List<ClinicResponse> getClinicById(Long clinicId) {
        return clinicRepository.findById(clinicId).stream()
                .map(this::toDto).toList();
    }

    public ClinicResponse toDto(Clinic savedClinic) {
        return new ClinicResponse(
                savedClinic.getId(),
                savedClinic.getName(),
                savedClinic.getWhatsappNumber(),
                savedClinic.getTimezone(),
                savedClinic.isActive(),
                savedClinic.getCountryCode(),
                savedClinic.getState(),
                savedClinic.getCity(),
                savedClinic.getPostalCode(),
                savedClinic.getAddressLine1(),
                savedClinic.getAddressLine2(),
                savedClinic.getLatitude(),
                savedClinic.getLongitude(),
                savedClinic.getCreatedAt());
    }
}