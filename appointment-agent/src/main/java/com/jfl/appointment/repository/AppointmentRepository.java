package com.jfl.appointment.repository;

import com.jfl.appointment.entity.Appointment;
import com.jfl.appointment.entity.AppointmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByDoctorIdAndAppointmentDateAndStatus(
            Long doctorId, LocalDate appointmentDate, AppointmentStatus status);

    Optional<Appointment> findByAppointmentCode(String appointmentCode);

    // Pessimistic lock on the doctor's rows for that date, taken BEFORE we
    // re-check availability + insert. This closes the race window between
    // "second check" and "create" described in the design doc (section 15/16):
    // two concurrent requests for the same doctor/date will serialize here.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a where a.doctor.id = :doctorId " +
            "and a.appointmentDate = :date and a.status = 'CONFIRMED'")
    List<Appointment> lockDoctorAppointmentsForDate(
            @Param("doctorId") Long doctorId, @Param("date") LocalDate date);


    // Powers the dashboard list view. JOIN FETCH pulls doctor/service/patient in
    // one query instead of N+1 lazy-loads per row when the DTO mapper touches them.
    @Query("select a from Appointment a " +
            "join fetch a.doctor join fetch a.service join fetch a.patient " +
            "where a.clinic.id = :clinicId " +
            "order by a.appointmentDate asc, a.startTime asc")
    List<Appointment> findForDashboard(
            @Param("clinicId") Long clinicId);

    @Query("""
            select a
            from Appointment a
            join fetch a.doctor
            join fetch a.service
            join fetch a.patient
            where a.clinic.id = :clinicId
              and (:serviceId is null or a.service.id = :serviceId)
              and (:doctorId is null or a.doctor.id = :doctorId)
              and (:status is null or a.status in :status)
            order by a.appointmentDate asc, a.startTime asc
            """)
    List<Appointment> findForDashboard(
            @Param("clinicId") Long clinicId,
            @Param("serviceId") Long serviceId,
            @Param("doctorId") Long doctorId,
            @Param("status") AppointmentStatus status
    );

    @Query("""
                SELECT COUNT(a)
                FROM Appointment a
                WHERE a.clinic.id = :clinicId
                  AND a.appointmentDate = :date
                  AND (:doctorId IS NULL OR a.doctor.id = :doctorId)
            """)
    long countForDashboard(
            @Param("clinicId") Long clinicId,
            @Param("date") LocalDate date,
            @Param("doctorId") Long doctorId
    );

//    @Query("""
//                SELECT COUNT(a)
//                FROM Appointment a
//                WHERE a.clinic.id = :clinicId
//                  AND a.appointmentDate = :date
//                  AND (:doctorId IS NULL OR a.doctor.id = :doctorId)
//                  AND a.status = com.jfl.appointment.entity.AppointmentStatus.PENDING
//            """)
//    long countPendingForDashboard(
//            @Param("clinicId") Long clinicId,
//            @Param("date") LocalDate date,
//            @Param("doctorId") Long doctorId
//    );

    @Query("""
                SELECT a
                FROM Appointment a
                JOIN FETCH a.patient
                JOIN FETCH a.doctor
                JOIN FETCH a.service
                WHERE a.clinic.id = :clinicId
                  AND a.appointmentDate = :date
                  AND (:doctorId IS NULL OR a.doctor.id = :doctorId)
                ORDER BY a.startTime ASC
            """)
    List<Appointment> findTodayForDashboard(
            @Param("clinicId") Long clinicId,
            @Param("date") LocalDate date,
            @Param("doctorId") Long doctorId
    );
}
