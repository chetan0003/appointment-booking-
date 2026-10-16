package com.jfl.appointment.repository;


import com.jfl.appointment.entity.ClinicQueueEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ClinicQueueEntryRepository
        extends JpaRepository<ClinicQueueEntry, Long> {

    Optional<ClinicQueueEntry> findByAppointment_Id(Long appointmentId);

    boolean existsByClinic_IdAndQueueDateAndQueueNumber(
            Long clinicId,
            LocalDate queueDate,
            Integer queueNumber
    );
}
