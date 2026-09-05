package com.jfl.appointment.dashboard.service;

import com.jfl.appointment.SlotUtil;
import com.jfl.appointment.entity.*;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.n8n.dto.AvailabilityResponse;
import com.jfl.appointment.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Turns (doctor, service, date) into a concrete list of bookable start times.
 * <p>
 * Algorithm:
 * 1. Load the clinic's working hours for that day-of-week (incl. break window).
 * 2. Walk the working window in service-duration-sized steps, skipping the break.
 * 3. Drop any step that overlaps an existing CONFIRMED appointment for that doctor.
 * 4. Drop any step in the past if the date is today.
 * <p>
 * This satisfies design doc section 15: this is the "first check" - a *second*,
 * authoritative check happens transactionally in DashboardAppointmentService right
 * before the row is inserted, so a slot returned here is never trusted blindly.
 */
@Service
@RequiredArgsConstructor
public class DashboardAvailabilityService {

    private final ClinicWorkingHoursRepository workingHoursRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final ServiceOfferingRepository serviceRepository;
    private final DoctorServiceRepository doctorServiceRepository;
    private final ClinicHolidayRepository clinicHolidayRepository;
    private final DoctorAvailabilityRepository doctorAvailabilityRepository;
    private final DoctorLeaveRepository doctorLeaveRepository;

    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailability(
            Long clinicId,
            Long doctorId,
            Long serviceId,
            LocalDate date) {

        Doctor doctor = doctorRepository.findById(doctorId)
                .filter(d ->
                        d.getClinic().getId().equals(clinicId)
                )
                .orElseThrow(() ->
                        new NotFoundException(
                                "Doctor not found: " + doctorId
                        ));

        ServiceOffering service = serviceRepository.findById(serviceId)
                .filter(s ->
                        s.getClinic().getId().equals(clinicId)
                )
                .orElseThrow(() ->
                        new NotFoundException(
                                "Service not found: " + serviceId
                        ));

        // Verify doctor can provide this service
        boolean doctorProvidesService =
                doctorServiceRepository
                        .existsByDoctorIdAndServiceId(
                                doctorId,
                                serviceId
                        );

        if (!doctorProvidesService) {
            throw new NotFoundException(
                    "Doctor " + doctorId +
                            " does not provide service " + serviceId
            );
        }

        List<LocalTime> slots =
                computeSlots(
                        clinicId,
                        doctor,
                        service,
                        date
                );

        return new AvailabilityResponse(
                date,
                slots
        );
    }

    public List<LocalTime> computeSlots(
            Long clinicId,
            Doctor doctor,
            ServiceOffering service,
            LocalDate date) {

        // --------------------------------------------------
        // 1. Clinic timezone
        // --------------------------------------------------
        ZoneId zoneId = ZoneId.of("Asia/Kolkata");

        LocalDate today = LocalDate.now(zoneId);

        LocalTime now = LocalTime.now(zoneId)
                .withSecond(0)
                .withNano(0);

        // --------------------------------------------------
        // 2. Past date -> no availability
        // --------------------------------------------------
        if (date.isBefore(today)) {
            return List.of();
        }

        // --------------------------------------------------
        // 3. Get day of week
        // --------------------------------------------------
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        // --------------------------------------------------
        // 4. Check clinic holiday
        // --------------------------------------------------
        boolean clinicHoliday =
                clinicHolidayRepository
                        .findByClinicIdAndHolidayDateAndActiveTrue(
                                clinicId,
                                date
                        )
                        .isPresent();

        if (clinicHoliday) {
            return List.of();
        }

        // --------------------------------------------------
        // 5. Get clinic working hours
        // --------------------------------------------------
        ClinicWorkingHours clinicHours =
                workingHoursRepository
                        .findByClinic_IdAndDayOfWeekAndActiveTrue(
                                clinicId,
                                dayOfWeek
                        )
                        .orElse(null);

        if (clinicHours == null) {
            // Clinic is closed on this day
            return List.of();
        }

        // --------------------------------------------------
        // 6. Get doctor availability
        // --------------------------------------------------
        DoctorAvailability doctorAvailability =
                doctorAvailabilityRepository
                        .findByDoctorIdAndDayOfWeekAndActiveTrue(
                                doctor.getId(),
                                dayOfWeek
                        )
                        .orElse(null);

        if (doctorAvailability == null) {
            // Doctor doesn't work on this day
            return List.of();
        }

        // --------------------------------------------------
        // 7. Calculate effective doctor working window
        // --------------------------------------------------
        LocalTime startTime = SlotUtil.max(
                clinicHours.getStartTime(),
                doctorAvailability.getStartTime()
        );

        LocalTime endTime = SlotUtil.min(
                clinicHours.getEndTime(),
                doctorAvailability.getEndTime()
        );

        // Doctor availability doesn't overlap clinic hours
        if (!startTime.isBefore(endTime)) {
            return List.of();
        }

        // --------------------------------------------------
        // 8. Get doctor leaves for this date
        // --------------------------------------------------
        List<DoctorLeave> doctorLeaves =
                doctorLeaveRepository
                        .findByDoctorIdAndLeaveDateAndActiveTrue(
                                doctor.getId(),
                                date
                        );

        // --------------------------------------------------
        // 9. Get existing appointments
        // --------------------------------------------------
        List<Appointment> existingAppointments =
                appointmentRepository
                        .findByDoctorIdAndAppointmentDate(
                                doctor.getId(),
                                date
                        );

        // --------------------------------------------------
        // 10. Service duration
        // --------------------------------------------------
        Integer durationMinutes = service.getDurationMinutes();

        if (durationMinutes == null || durationMinutes <= 0) {
            return List.of();
        }

        // --------------------------------------------------
        // 11. Slot interval
        //
        // Example:
        // Service duration = 45 minutes
        // Slot interval    = 30 minutes
        //
        // Slots:
        // 09:00 -> 09:45
        // 09:30 -> 10:15
        // 10:00 -> 10:45
        // --------------------------------------------------
        final int slotIntervalMinutes = 30;

        // --------------------------------------------------
        // 12. Generate slots
        // --------------------------------------------------
        List<LocalTime> slots = new ArrayList<>();

        LocalTime cursor = startTime;

        while (!cursor.plusMinutes(durationMinutes).isAfter(endTime)) {

            LocalTime slotStart = cursor;

            LocalTime slotEnd =
                    slotStart.plusMinutes(durationMinutes);

            // --------------------------------------------------
            // 13. Clinic break
            // --------------------------------------------------
            boolean overlapsClinicBreak =
                    SlotUtil.overlaps(
                            slotStart,
                            slotEnd,
                            clinicHours.getBreakStartTime(),
                            clinicHours.getBreakEndTime()
                    );

            // --------------------------------------------------
            // 14. Doctor break
            // --------------------------------------------------
            boolean overlapsDoctorBreak =
                    SlotUtil.overlaps(
                            slotStart,
                            slotEnd,
                            doctorAvailability.getBreakStartTime(),
                            doctorAvailability.getBreakEndTime()
                    );

            // --------------------------------------------------
            // 15. Doctor leave
            // --------------------------------------------------
            boolean overlapsDoctorLeave =
                    doctorLeaves.stream()
                            .anyMatch(leave ->
                                    SlotUtil.overlaps(
                                            slotStart,
                                            slotEnd,
                                            leave.getStartTime(),
                                            leave.getEndTime()
                                    )
                            );

            // --------------------------------------------------
            // 16. Existing appointment
            // --------------------------------------------------
            boolean alreadyBooked =
                    existingAppointments.stream()
                            .anyMatch(appointment ->
                                    SlotUtil.overlaps(
                                            slotStart,
                                            slotEnd,
                                            appointment.getStartTime(),
                                            appointment.getEndTime()
                                    )
                            );

            // --------------------------------------------------
            // 17. Current/past slot
            //
            // If current time is 12:00:
            //
            // 11:30 -> excluded
            // 12:00 -> excluded
            // 12:30 -> allowed
            //
            // If current time is 12:17:
            //
            // 12:00 -> excluded
            // 12:30 -> allowed
            // --------------------------------------------------
            boolean currentOrPastSlot =
                    date.equals(today)
                            && !slotStart.isAfter(now);

            // --------------------------------------------------
            // 18. Final availability check
            // --------------------------------------------------
            if (!overlapsClinicBreak
                    && !overlapsDoctorBreak
                    && !overlapsDoctorLeave
                    && !alreadyBooked
                    && !currentOrPastSlot) {

                slots.add(slotStart);
            }

            // --------------------------------------------------
            // 19. Move to next slot
            //
            // IMPORTANT:
            // Move by slot interval, not service duration.
            // --------------------------------------------------
            cursor = cursor.plusMinutes(slotIntervalMinutes);
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
