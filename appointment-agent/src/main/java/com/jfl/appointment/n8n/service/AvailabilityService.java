package com.jfl.appointment.n8n.service;

import com.jfl.appointment.n8n.dto.AvailabilityResponse;
import com.jfl.appointment.entity.*;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.repository.AppointmentRepository;
import com.jfl.appointment.repository.ClinicWorkingHoursRepository;
import com.jfl.appointment.repository.DoctorRepository;
import com.jfl.appointment.repository.ServiceOfferingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Turns (doctor, service, date) into a concrete list of bookable start times.
 *
 * Algorithm:
 *   1. Load the clinic's working hours for that day-of-week (incl. break window).
 *   2. Walk the working window in service-duration-sized steps, skipping the break.
 *   3. Drop any step that overlaps an existing CONFIRMED appointment for that doctor.
 *   4. Drop any step in the past if the date is today.
 *
 * This satisfies design doc section 15: this is the "first check" - a *second*,
 * authoritative check happens transactionally in DashboardAppointmentService right
 * before the row is inserted, so a slot returned here is never trusted blindly.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final ClinicWorkingHoursRepository workingHoursRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final ServiceOfferingRepository serviceRepository;

    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailability(Long clinicId, Long doctorId, Long serviceId, LocalDate date) {

        Doctor doctor = doctorRepository.findById(doctorId)
                .filter(d -> d.getClinic().getId().equals(clinicId))
                .orElseThrow(() -> new NotFoundException("Doctor not found: " + doctorId));

        ServiceOffering service = serviceRepository.findById(serviceId)
                .filter(s -> s.getClinic().getId().equals(clinicId))
                .orElseThrow(() -> new NotFoundException("Service not found: " + serviceId));

        List<LocalTime> slots = computeSlots(clinicId, doctor, service, date);
        return new AvailabilityResponse(date, slots);
    }

    public List<LocalTime> computeSlots(Long clinicId, Doctor doctor, ServiceOffering service, LocalDate date) {
        ClinicWorkingHours hours = workingHoursRepository
                .findByClinic_IdAndDayOfWeekAndActiveTrue(clinicId, date.getDayOfWeek())
                .orElse(null);

        if (hours == null) {
            return List.of(); // clinic closed that day
        }

        int durationMinutes = service.getDurationMinutes();

        List<Appointment> existing = appointmentRepository.findByDoctorIdAndAppointmentDateAndStatus(
                doctor.getId(), date, AppointmentStatus.CONFIRMED);

        Set<LocalTime> bookedStarts = existing.stream()
                .map(Appointment::getStartTime)
                .collect(Collectors.toSet());

        boolean isToday = date.equals(LocalDate.now());
        LocalTime now = LocalTime.now();

        List<LocalTime> slots = new ArrayList<>();
        LocalTime cursor = hours.getStartTime();

        while (!cursor.plusMinutes(durationMinutes).isAfter(hours.getEndTime())) {
            LocalTime slotEnd = cursor.plusMinutes(durationMinutes);

            boolean overlapsBreak = hours.getBreakStartTime() != null
                    && cursor.isBefore(hours.getBreakEndTime())
                    && slotEnd.isAfter(hours.getBreakStartTime());

            boolean alreadyBooked = bookedStarts.contains(cursor);
            boolean isPast = isToday && cursor.isBefore(now);

            if (!overlapsBreak && !alreadyBooked && !isPast) {
                slots.add(cursor);
            }

            cursor = cursor.plusMinutes(durationMinutes);
        }

        return slots;
    }

    /**
     * @param excludeAppointmentId when rescheduling an existing appointment, pass its id so its
     *                             own (soon-to-be-vacated) slot doesn't count as "booked" against itself.
     *                             Pass null for a fresh booking.
     */
    public List<LocalTime> computeSlots(Long clinicId, Doctor doctor, ServiceOffering service, LocalDate date,
                                        Long excludeAppointmentId) {
        ClinicWorkingHours hours = workingHoursRepository
                .findByClinicIdAndDayOfWeekAndActiveTrue(clinicId, date.getDayOfWeek())
                .orElse(null);

        if (hours == null) {
            return List.of(); // clinic closed that day
        }

        int durationMinutes = service.getDurationMinutes();

        List<Appointment> existing = appointmentRepository.findByDoctorIdAndAppointmentDateAndStatus(
                doctor.getId(), date, AppointmentStatus.CONFIRMED);

        Set<LocalTime> bookedStarts = existing.stream()
                .filter(a -> excludeAppointmentId == null || !a.getId().equals(excludeAppointmentId))
                .map(Appointment::getStartTime)
                .collect(Collectors.toSet());

        boolean isToday = date.equals(LocalDate.now());
        LocalTime now = LocalTime.now();

        List<LocalTime> slots = new ArrayList<>();
        LocalTime cursor = hours.getStartTime();

        while (!cursor.plusMinutes(durationMinutes).isAfter(hours.getEndTime())) {
            LocalTime slotEnd = cursor.plusMinutes(durationMinutes);

            boolean overlapsBreak = hours.getBreakStartTime() != null
                    && cursor.isBefore(hours.getBreakEndTime())
                    && slotEnd.isAfter(hours.getBreakStartTime());

            boolean alreadyBooked = bookedStarts.contains(cursor);
            boolean isPast = isToday && cursor.isBefore(now);

            if (!overlapsBreak && !alreadyBooked && !isPast) {
                slots.add(cursor);
            }

            cursor = cursor.plusMinutes(durationMinutes);
        }

        return slots;
    }
}
