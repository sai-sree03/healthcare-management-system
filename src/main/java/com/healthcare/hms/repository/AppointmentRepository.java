package com.healthcare.hms.repository;

import com.healthcare.hms.entity.Appointment;
import com.healthcare.hms.entity.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @EntityGraph(attributePaths = {"patient", "doctor"})
    Optional<Appointment> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"patient", "doctor"})
    @Query("""
            select a from Appointment a
            where (:doctorId is null or a.doctor.id = :doctorId)
              and (:patientId is null or a.patient.id = :patientId)
              and (:date is null or a.appointmentDate = :date)
              and (:status is null or a.status = :status)
            """)
    Page<Appointment> search(@Param("doctorId") Long doctorId,
                             @Param("patientId") Long patientId,
                             @Param("date") LocalDate date,
                             @Param("status") AppointmentStatus status,
                             Pageable pageable);

    /** True if the doctor already has a non-cancelled visit starting strictly between after and before. */
    @Query("""
            select count(a) > 0 from Appointment a
            where a.doctor.id = :doctorId
              and a.appointmentDate = :date
              and a.status <> :cancelled
              and a.startTime > :after and a.startTime < :before
              and a.id <> :excludeId
            """)
    boolean existsDoctorConflict(@Param("doctorId") Long doctorId,
                                 @Param("date") LocalDate date,
                                 @Param("after") LocalTime after,
                                 @Param("before") LocalTime before,
                                 @Param("cancelled") AppointmentStatus cancelled,
                                 @Param("excludeId") Long excludeId);

    /** Same check for the patient, so one person cannot be booked in two places at once. */
    @Query("""
            select count(a) > 0 from Appointment a
            where a.patient.id = :patientId
              and a.appointmentDate = :date
              and a.status <> :cancelled
              and a.startTime > :after and a.startTime < :before
              and a.id <> :excludeId
            """)
    boolean existsPatientConflict(@Param("patientId") Long patientId,
                                  @Param("date") LocalDate date,
                                  @Param("after") LocalTime after,
                                  @Param("before") LocalTime before,
                                  @Param("cancelled") AppointmentStatus cancelled,
                                  @Param("excludeId") Long excludeId);

    boolean existsByPatient_IdAndStatus(Long patientId, AppointmentStatus status);

    boolean existsByDoctor_IdAndStatus(Long doctorId, AppointmentStatus status);

    long countByAppointmentDateAndStatusNot(LocalDate date, AppointmentStatus status);

    long countByAppointmentDateAndStatusNotAndDoctor_Id(LocalDate date, AppointmentStatus status, Long doctorId);

    long countByStatus(AppointmentStatus status);

    long countByStatusAndDoctor_Id(AppointmentStatus status, Long doctorId);
}
