package com.jfl.appointment.dashboard.service;


import com.jfl.appointment.dashboard.dto.ApiDashboardResponse;
import com.jfl.appointment.dashboard.dto.AppointmentDashboardItem;
import com.jfl.appointment.entity.Appointment;
import com.jfl.appointment.entity.ClinicUser;
import com.jfl.appointment.repository.AppointmentRepository;
import com.jfl.appointment.repository.DoctorRepository;
import com.jfl.appointment.repository.PatientRepository;
import com.jfl.appointment.security.SecurityContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final ClinicAccessService clinicAccessService;
    private final SecurityContextService securityContextService;

    public ApiDashboardResponse.DashboardResponse getDashboard(Long clinicId) {

        if (!clinicAccessService.hasAccessToClinic(clinicId)) {
            throw new SecurityException(
                    "User does not have access to clinic: " + clinicId);
        }

        Long effectiveDoctorId = resolveDoctorId(clinicId);
        LocalDate today = LocalDate.now();

        long todayAppointments =
                appointmentRepository.countForDashboard(
                        clinicId, today, effectiveDoctorId);

//        long pendingAppointments =
//                appointmentRepository.countPendingForDashboard(
//                        clinicId, today, effectiveDoctorId);

        long totalPatients =
                patientRepository.countByClinicId(clinicId);

        long activeDoctors =
                doctorRepository.countByClinicIdAndActiveTrue(clinicId);

        List<AppointmentDashboardItem> schedule =
                appointmentRepository
                        .findTodayForDashboard(
                                clinicId, today, effectiveDoctorId)
                        .stream()
                        .map(this::toDashboardItem)
                        .toList();

        return new ApiDashboardResponse.DashboardResponse(
                todayAppointments,
                0,//pendingAppointments
                totalPatients,
                activeDoctors,
                schedule
        );
    }

    private Long resolveDoctorId(Long clinicId) {

        if (!securityContextService.hasRole("DOCTOR")) {
            return null;
        }

        ClinicUser membership =
                clinicAccessService.getClinicUser(clinicId);

        if (membership.getDoctor() == null) {
            throw new SecurityException(
                    "Doctor user is not linked to a doctor record");
        }

        return membership.getDoctor().getId();
    }

    private AppointmentDashboardItem toDashboardItem(
            Appointment appointment) {

        return new AppointmentDashboardItem(
                appointment.getId(),
                appointment.getPatient().getName(),
                appointment.getDoctor().getName(),
                appointment.getService().getName(),
                appointment.getAppointmentDate(),
                appointment.getStartTime(),
                appointment.getStatus().name()
        );
    }


}

