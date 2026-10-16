package com.jfl.appointment.repository;

import com.jfl.appointment.entity.Appointment;
import com.jfl.appointment.entity.AppointmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long>,
        JpaSpecificationExecutor<Appointment> {

    Optional<Appointment> findByIdAndClinicId(Long appointmentId, Long clinicId);
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

              and (
                  coalesce(:from, null) is null
                  or a.appointmentDate >= :from
              )

              and (
                  coalesce(:to, null) is null
                  or a.appointmentDate <= :to
              )

              and (
                  :serviceId is null
                  or a.service.id = :serviceId
              )

              and (
                  :doctorId is null
                  or a.doctor.id = :doctorId
              )

              and (
                  :status is null
                  or a.status = :status
              )

            order by a.appointmentDate asc, a.startTime asc
            """)
    Page<Appointment> findForDashboard(
            @Param("clinicId") Long clinicId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("doctorId") Long doctorId,
            @Param("serviceId") Long serviceId,
            @Param("status") AppointmentStatus status,
            Pageable pageable
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

    List<Appointment> findByDoctorIdAndAppointmentDateAndStatusIn(
            Long doctorId,
            LocalDate date,
            Collection<AppointmentStatus> statuses
    );

    List<Appointment> findByDoctorIdAndAppointmentDate(
            Long doctorId,
            LocalDate appointmentDate
    );

    List<Appointment> findByFollowUpOfAppointmentId(
            Long appointmentId
    );

    @Query("""
            select count(a) > 0
            from Appointment a
            where a.doctor.id = :doctorId
              and a.clinic.id = :clinicId
              and a.appointmentDate = :date
              and a.id <> :appointmentId
              and a.status not in (
                    com.jfl.appointment.entity.AppointmentStatus.CANCELLED,
                    com.jfl.appointment.entity.AppointmentStatus.NO_SHOW
              )
              and a.startTime < :endTime
              and a.endTime > :startTime
            """)
    boolean existsConflictForReschedule(
            @Param("doctorId") Long doctorId,
            @Param("clinicId") Long clinicId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("appointmentId") Long appointmentId
    );

    @Query("""
            select count(a) > 0
            from Appointment a
            where a.doctor.id = :doctorId
              and a.appointmentDate = :date
              and a.status not in (
                    com.jfl.appointment.entity.AppointmentStatus.CANCELLED,
                    com.jfl.appointment.entity.AppointmentStatus.NO_SHOW
              )
              and a.startTime < :endTime
              and a.endTime > :startTime
            """)
    boolean existsConflict(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    Page<Appointment> findByClinicIdAndPatientIdOrderByAppointmentDateAscStartTimeAsc(
            Long clinicId,
            Long patientId,
            Pageable pageable
    );

    @Query("""
            SELECT a.appointmentDate, COUNT(a)
            FROM Appointment a
            WHERE a.clinic.id = :clinicId
              AND a.appointmentDate BETWEEN :fromDate AND :toDate
              AND a.status NOT IN (
                  com.jfl.appointment.entity.AppointmentStatus.CANCELLED,
                  com.jfl.appointment.entity.AppointmentStatus.NO_SHOW
              )
            GROUP BY a.appointmentDate
            ORDER BY a.appointmentDate
            """)
    List<Object[]> countAppointmentsByDate(
            @Param("clinicId") Long clinicId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query("""
            SELECT COUNT(a)
            FROM Appointment a
            WHERE a.clinic.id = :clinicId
              AND a.appointmentDate >= :startDate
              AND a.appointmentDate <= :endDate
            """)
    long countAppointmentsForPeriod(
            @Param("clinicId") Long clinicId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT a
            FROM Appointment a
            WHERE a.clinic.id = :clinicId
              AND a.patient.id = :patientId
              AND a.doctor.id = :doctorId
            ORDER BY a.appointmentDate ASC, a.startTime ASC
            """)
    Page<Appointment> findPatientAppointmentsByDoctor(
            @Param("clinicId") Long clinicId,
            @Param("patientId") Long patientId,
            @Param("doctorId") Long doctorId,
            Pageable pageable
    );

    @Query("""
            SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
            FROM Appointment a
            WHERE a.whatsappNumber = :whatsappNumber
              AND a.clinic.id = :clinicId
              AND a.appointmentDate = :appointmentDate
              AND a.startTime = :startTime
              AND a.status IN :statuses
            """)
    boolean existsDuplicatePhoneSlot(
            @Param("whatsappNumber") String whatsappNumber,
            @Param("clinicId") Long clinicId,
            @Param("appointmentDate") LocalDate appointmentDate,
            @Param("startTime") LocalTime startTime,
            @Param("statuses") Collection<AppointmentStatus> statuses
    );

    boolean existsByWhatsappNumberAndClinic_IdAndAppointmentDateAndStartTimeAndStatusIn(
            String whatsappNumber,
            Long clinicId,
            LocalDate appointmentDate,
            LocalTime startTime,
            Collection<AppointmentStatus> statuses
    );

    /**
     * Successful bookings created today for this phone + clinic.
     * Uses createdAt (AuditableEntity) so "3 per calendar day" = jab book kiya, not appointmentDate.
     */
    @Query("""
            SELECT COUNT(a) FROM Appointment a
            WHERE a.clinic.id = :clinicId
              AND a.whatsappNumber = :whatsappNumber
              AND a.status IN :statuses
              AND a.createdAt >= :startOfDay
              AND a.createdAt < :endOfDay
            """)
    long countSuccessfulCreatedToday(
            @Param("whatsappNumber") String whatsappNumber,
            @Param("clinicId") Long clinicId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay,
            @Param("statuses") List<AppointmentStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Appointment a WHERE a.id = :appointmentId")
    Optional<Appointment> findByIdForUpdate(
            @Param("appointmentId") Long appointmentId
    );

    @Query("""
            SELECT COUNT(a)
            FROM Appointment a
            WHERE a.patient.id = :patientId
              AND a.clinic.id = :clinicId
              AND a.appointmentDate = :appointmentDate
              AND a.status IN :statuses
            """)
    int countPatientAppointmentsForDate(
            @Param("patientId") Long patientId,
            @Param("clinicId") Long clinicId,
            @Param("appointmentDate") LocalDate appointmentDate,
            @Param("statuses") Collection<AppointmentStatus> statuses
    );
}
