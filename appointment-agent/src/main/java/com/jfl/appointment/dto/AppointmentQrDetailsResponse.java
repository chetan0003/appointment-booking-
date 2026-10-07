package com.jfl.appointment.dto;

import com.jfl.appointment.entity.AppointmentQrStatus;
import com.jfl.appointment.entity.AppointmentStatus;
import com.jfl.appointment.entity.PatientProfileStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record AppointmentQrDetailsResponse(
        Long appointmentId,
        String AppointmentCode,
        Long patientId,
        String patientName,
        PatientProfileStatus patientProfileStatus,
        String doctorName,
        String clinicName,
        String serviceName,
        LocalDate appointmentDate,
        LocalTime appointmentTime,
        AppointmentStatus appointmentStatus,
        AppointmentQrStatus qrStatus,
        LocalDateTime qrExpiresAt
) {
}
