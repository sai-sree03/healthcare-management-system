package com.healthcare.hms.service;

import com.healthcare.hms.dto.AppointmentDto;
import com.healthcare.hms.dto.PageResponse;
import com.healthcare.hms.entity.Appointment;
import com.healthcare.hms.entity.AppointmentStatus;
import com.healthcare.hms.entity.Doctor;
import com.healthcare.hms.entity.Patient;
import com.healthcare.hms.entity.Role;
import com.healthcare.hms.exception.BadRequestException;
import com.healthcare.hms.exception.ConflictException;
import com.healthcare.hms.exception.ResourceNotFoundException;
import com.healthcare.hms.repository.AppointmentRepository;
import com.healthcare.hms.repository.DoctorRepository;
import com.healthcare.hms.repository.PatientRepository;
import com.healthcare.hms.security.CurrentUser;
import com.healthcare.hms.security.HmsUserDetails;
import com.healthcare.hms.util.Paging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class AppointmentService {

    /** Used by the conflict queries when there is no appointment to exclude (i.e. on create). */
    private static final long NO_EXCLUDED_ID = -1L;

    private final AppointmentRepository appointments;
    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final Clock clock;
    private final int slotMinutes;

    public AppointmentService(AppointmentRepository appointments,
                              PatientRepository patients,
                              DoctorRepository doctors,
                              Clock clock,
                              @Value("${hms.appointment.slot-minutes:30}") int slotMinutes) {
        this.appointments = appointments;
        this.patients = patients;
        this.doctors = doctors;
        this.clock = clock;
        this.slotMinutes = slotMinutes;
    }

    // ------------------------------------------------------------------ reads

    @Transactional(readOnly = true)
    public PageResponse<AppointmentDto.Response> list(Long doctorId, Long patientId, LocalDate date,
                                                      AppointmentStatus status, int page, int size) {
        HmsUserDetails user = CurrentUser.get();
        Long effectiveDoctorId = doctorId;
        if (user.getRole() == Role.DOCTOR) {
            // a doctor only ever sees their own schedule, whatever the client asks for
            effectiveDoctorId = requireDoctorId(user);
        }
        var pageable = Paging.of(page, size,
                Sort.by(Sort.Order.desc("appointmentDate"), Sort.Order.desc("startTime")));
        return PageResponse.from(appointments.search(effectiveDoctorId, patientId, date, status, pageable),
                this::toResponse);
    }

    @Transactional(readOnly = true)
    public AppointmentDto.Response get(Long id) {
        Appointment appointment = load(id);
        assertCanSee(CurrentUser.get(), appointment);
        return toResponse(appointment);
    }

    // ------------------------------------------------------------------ writes

    @Transactional
    public AppointmentDto.Response create(AppointmentDto.Request request) {
        Patient patient = patients.findByIdAndActiveTrue(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found."));
        Doctor doctor = doctors.findByIdAndActiveTrue(request.doctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found."));

        assertNotInPast(request.appointmentDate(), request.startTime());
        assertSlotAvailable(patient, doctor, request.appointmentDate(), request.startTime(), NO_EXCLUDED_ID);

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDate(request.appointmentDate());
        appointment.setStartTime(request.startTime());
        appointment.setReason(request.reason().trim());
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        return toResponse(appointments.save(appointment));
    }

    @Transactional
    public AppointmentDto.Response update(Long id, AppointmentDto.Request request) {
        Appointment appointment = load(id);
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new BadRequestException("Only scheduled appointments can be edited.");
        }
        Patient patient = patients.findByIdAndActiveTrue(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found."));
        Doctor doctor = doctors.findByIdAndActiveTrue(request.doctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found."));

        boolean timeChanged = !appointment.getAppointmentDate().equals(request.appointmentDate())
                || !appointment.getStartTime().equals(request.startTime());
        if (timeChanged) {
            assertNotInPast(request.appointmentDate(), request.startTime());
        }
        assertSlotAvailable(patient, doctor, request.appointmentDate(), request.startTime(), appointment.getId());

        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDate(request.appointmentDate());
        appointment.setStartTime(request.startTime());
        appointment.setReason(request.reason().trim());
        return toResponse(appointments.save(appointment));
    }

    /**
     * Role rules for status changes:
     *  - RECEPTIONIST may only cancel
     *  - DOCTOR may complete or cancel, but only their own appointments
     *  - ADMIN may do both
     * A finished (completed/cancelled) appointment can never change again.
     */
    @Transactional
    public AppointmentDto.Response changeStatus(Long id, AppointmentStatus newStatus) {
        HmsUserDetails user = CurrentUser.get();
        Appointment appointment = load(id);

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new BadRequestException("This appointment is already " + appointment.getStatus().name().toLowerCase() + ".");
        }
        if (newStatus == AppointmentStatus.SCHEDULED) {
            throw new BadRequestException("Use the edit option to change a scheduled appointment.");
        }

        switch (user.getRole()) {
            case RECEPTIONIST -> {
                if (newStatus != AppointmentStatus.CANCELLED) {
                    throw new AccessDeniedException("Receptionists can only cancel appointments.");
                }
            }
            case DOCTOR -> assertOwnedByDoctor(user, appointment);
            case ADMIN -> { /* allowed */ }
        }

        appointment.setStatus(newStatus);
        return toResponse(appointments.save(appointment));
    }

    // ------------------------------------------------------------------ rules

    private void assertNotInPast(LocalDate date, LocalTime time) {
        if (LocalDateTime.of(date, time).isBefore(LocalDateTime.now(clock))) {
            throw new BadRequestException("Appointments cannot be booked in the past.");
        }
    }

    private void assertSlotAvailable(Patient patient, Doctor doctor, LocalDate date, LocalTime start, long excludeId) {
        LocalTime end = start.plusMinutes(slotMinutes);
        boolean wrapsPastMidnight = end.isBefore(start) || end.equals(LocalTime.MIDNIGHT);
        if (wrapsPastMidnight || start.isBefore(doctor.getAvailableFrom()) || end.isAfter(doctor.getAvailableTo())) {
            throw new BadRequestException("The doctor is only available between "
                    + doctor.getAvailableFrom() + " and " + doctor.getAvailableTo()
                    + " (" + slotMinutes + "-minute visits).");
        }

        // Any visit starting within one slot before or after (including the same minute) overlaps this one.
        LocalTime after = start.minusMinutes(slotMinutes);
        LocalTime before = start.plusMinutes(slotMinutes);
        if (appointments.existsDoctorConflict(doctor.getId(), date, after, before, AppointmentStatus.CANCELLED, excludeId)) {
            throw new ConflictException("This doctor already has an appointment at that time.");
        }
        if (appointments.existsPatientConflict(patient.getId(), date, after, before, AppointmentStatus.CANCELLED, excludeId)) {
            throw new ConflictException("This patient already has an appointment at that time.");
        }
    }

    private static void assertCanSee(HmsUserDetails user, Appointment appointment) {
        if (user.getRole() == Role.DOCTOR) {
            assertOwnedByDoctor(user, appointment);
        }
    }

    private static void assertOwnedByDoctor(HmsUserDetails user, Appointment appointment) {
        Long ownDoctorId = requireDoctorId(user);
        if (!ownDoctorId.equals(appointment.getDoctor().getId())) {
            throw new AccessDeniedException("You can only work with your own appointments.");
        }
    }

    static Long requireDoctorId(HmsUserDetails user) {
        if (user.getDoctorId() == null) {
            throw new AccessDeniedException("Your login is not linked to a doctor profile. Ask an administrator.");
        }
        return user.getDoctorId();
    }

    // ------------------------------------------------------------------ helpers

    private Appointment load(Long id) {
        return appointments.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found."));
    }

    private AppointmentDto.Response toResponse(Appointment a) {
        return new AppointmentDto.Response(
                a.getId(),
                a.getPatient().getId(), a.getPatient().getFullName(),
                a.getDoctor().getId(), a.getDoctor().getFullName(),
                a.getAppointmentDate(), a.getStartTime(), a.getStartTime().plusMinutes(slotMinutes),
                a.getReason(), a.getStatus());
    }
}
