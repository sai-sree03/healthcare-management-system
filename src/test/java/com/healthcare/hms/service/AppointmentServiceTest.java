package com.healthcare.hms.service;

import com.healthcare.hms.TestUsers;
import com.healthcare.hms.dto.AppointmentDto;
import com.healthcare.hms.entity.Appointment;
import com.healthcare.hms.entity.AppointmentStatus;
import com.healthcare.hms.entity.Doctor;
import com.healthcare.hms.entity.Patient;
import com.healthcare.hms.entity.Role;
import com.healthcare.hms.exception.BadRequestException;
import com.healthcare.hms.exception.ConflictException;
import com.healthcare.hms.repository.AppointmentRepository;
import com.healthcare.hms.repository.DoctorRepository;
import com.healthcare.hms.repository.PatientRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppointmentServiceTest {

    // "now" is fixed to 2026-10-08 10:00 so the tests never depend on the real date
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-08T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TOMORROW = LocalDate.of(2026, 10, 9);

    private AppointmentRepository appointments;
    private PatientRepository patients;
    private DoctorRepository doctors;
    private AppointmentService service;

    private Patient patient;
    private Doctor doctor;

    @BeforeEach
    void setUp() {
        appointments = mock(AppointmentRepository.class);
        patients = mock(PatientRepository.class);
        doctors = mock(DoctorRepository.class);
        service = new AppointmentService(appointments, patients, doctors, CLOCK, 30);

        patient = new Patient();
        patient.setId(1L);
        patient.setFullName("Ravi Kumar");

        doctor = new Doctor();
        doctor.setId(5L);
        doctor.setFullName("Dr. Rao");
        doctor.setAvailableFrom(LocalTime.of(9, 0));
        doctor.setAvailableTo(LocalTime.of(17, 0));

        when(patients.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(patient));
        when(doctors.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(doctor));
        when(appointments.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        TestUsers.logout();
    }

    private AppointmentDto.Request request(LocalDate date, LocalTime time) {
        return new AppointmentDto.Request(1L, 5L, date, time, "Check-up");
    }

    private Appointment scheduledAppointmentFor(Doctor owner) {
        Appointment a = new Appointment();
        a.setId(10L);
        a.setPatient(patient);
        a.setDoctor(owner);
        a.setAppointmentDate(TOMORROW);
        a.setStartTime(LocalTime.of(10, 0));
        a.setReason("Check-up");
        a.setStatus(AppointmentStatus.SCHEDULED);
        when(appointments.findWithDetailsById(10L)).thenReturn(Optional.of(a));
        return a;
    }

    // ---------------------------------------------------------------- booking rules

    @Test
    void booksAFreeSlot() {
        AppointmentDto.Response response = service.create(request(TOMORROW, LocalTime.of(10, 0)));
        assertEquals(AppointmentStatus.SCHEDULED, response.status());
        assertEquals(LocalTime.of(10, 30), response.endTime());
    }

    @Test
    void rejectsAppointmentsInThePast() {
        assertThrows(BadRequestException.class,
                () -> service.create(request(LocalDate.of(2026, 10, 7), LocalTime.of(10, 0))));
    }

    @Test
    void rejectsTimesOutsideDoctorHours() {
        assertThrows(BadRequestException.class, () -> service.create(request(TOMORROW, LocalTime.of(8, 0))));
        // 16:45 + 30 minutes would run past the 17:00 end of the doctor's day
        assertThrows(BadRequestException.class, () -> service.create(request(TOMORROW, LocalTime.of(16, 45))));
    }

    @Test
    void rejectsDoubleBookingTheDoctor() {
        when(appointments.existsDoctorConflict(anyLong(), any(LocalDate.class), any(LocalTime.class),
                any(LocalTime.class), any(AppointmentStatus.class), anyLong())).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.create(request(TOMORROW, LocalTime.of(10, 0))));
    }

    @Test
    void rejectsDoubleBookingThePatient() {
        when(appointments.existsPatientConflict(anyLong(), any(LocalDate.class), any(LocalTime.class),
                any(LocalTime.class), any(AppointmentStatus.class), anyLong())).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.create(request(TOMORROW, LocalTime.of(10, 0))));
    }

    // ---------------------------------------------------------------- role rules for status changes

    @Test
    void receptionistCanCancelButNotComplete() {
        scheduledAppointmentFor(doctor);
        TestUsers.loginAs(Role.RECEPTIONIST, null);

        assertThrows(AccessDeniedException.class, () -> service.changeStatus(10L, AppointmentStatus.COMPLETED));
        assertEquals(AppointmentStatus.CANCELLED, service.changeStatus(10L, AppointmentStatus.CANCELLED).status());
    }

    @Test
    void doctorCanCompleteTheirOwnAppointment() {
        scheduledAppointmentFor(doctor);
        TestUsers.loginAs(Role.DOCTOR, 5L);

        assertEquals(AppointmentStatus.COMPLETED, service.changeStatus(10L, AppointmentStatus.COMPLETED).status());
    }

    @Test
    void doctorCannotTouchAnotherDoctorsAppointment() {
        scheduledAppointmentFor(doctor);          // belongs to doctor 5
        TestUsers.loginAs(Role.DOCTOR, 99L);      // a different doctor

        assertThrows(AccessDeniedException.class, () -> service.changeStatus(10L, AppointmentStatus.COMPLETED));
        assertThrows(AccessDeniedException.class, () -> service.get(10L));
    }

    @Test
    void doctorWithoutLinkedProfileIsRefused() {
        scheduledAppointmentFor(doctor);
        TestUsers.loginAs(Role.DOCTOR, null);

        assertThrows(AccessDeniedException.class, () -> service.changeStatus(10L, AppointmentStatus.COMPLETED));
    }

    @Test
    void finishedAppointmentsCannotChangeAgain() {
        Appointment a = scheduledAppointmentFor(doctor);
        a.setStatus(AppointmentStatus.CANCELLED);
        TestUsers.loginAs(Role.ADMIN, null);

        assertThrows(BadRequestException.class, () -> service.changeStatus(10L, AppointmentStatus.COMPLETED));
    }
}
