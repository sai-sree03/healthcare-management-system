package com.healthcare.hms.service;

import com.healthcare.hms.TestUsers;
import com.healthcare.hms.dto.MedicalRecordDto;
import com.healthcare.hms.entity.Doctor;
import com.healthcare.hms.entity.MedicalRecord;
import com.healthcare.hms.entity.Patient;
import com.healthcare.hms.entity.Role;
import com.healthcare.hms.repository.AppointmentRepository;
import com.healthcare.hms.repository.DoctorRepository;
import com.healthcare.hms.repository.MedicalRecordRepository;
import com.healthcare.hms.repository.PatientRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MedicalRecordServiceTest {

    private MedicalRecordRepository records;
    private PatientRepository patients;
    private DoctorRepository doctors;
    private MedicalRecordService service;

    private Patient patient;
    private Doctor doctor;

    @BeforeEach
    void setUp() {
        records = mock(MedicalRecordRepository.class);
        patients = mock(PatientRepository.class);
        doctors = mock(DoctorRepository.class);
        service = new MedicalRecordService(records, patients, doctors, mock(AppointmentRepository.class));

        patient = new Patient();
        patient.setId(1L);
        patient.setFullName("Ravi Kumar");

        doctor = new Doctor();
        doctor.setId(5L);
        doctor.setFullName("Dr. Rao");

        when(patients.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(patient));
        when(doctors.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(doctor));
        when(records.save(any(MedicalRecord.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        TestUsers.logout();
    }

    private MedicalRecordDto.Request request() {
        return new MedicalRecordDto.Request(1L, null, "Flu", "Rest and fluids", "Paracetamol", LocalDate.of(2026, 10, 1));
    }

    private MedicalRecord existingRecordWrittenBy(Doctor author) {
        MedicalRecord record = new MedicalRecord();
        record.setId(7L);
        record.setPatient(patient);
        record.setDoctor(author);
        record.setDiagnosis("Flu");
        record.setTreatment("Rest");
        record.setRecordDate(LocalDate.of(2026, 10, 1));
        when(records.findWithDetailsByIdAndActiveTrue(7L)).thenReturn(Optional.of(record));
        return record;
    }

    @Test
    void authorIsAlwaysTheLoggedInDoctor() {
        TestUsers.loginAs(Role.DOCTOR, 5L);

        MedicalRecordDto.Response response = service.create(request());

        assertEquals(5L, response.doctorId());
        assertEquals("Dr. Rao", response.doctorName());
    }

    @Test
    void doctorWithoutLinkedProfileCannotCreate() {
        TestUsers.loginAs(Role.DOCTOR, null);
        assertThrows(AccessDeniedException.class, () -> service.create(request()));
    }

    @Test
    void doctorCanEditTheirOwnRecord() {
        existingRecordWrittenBy(doctor);
        TestUsers.loginAs(Role.DOCTOR, 5L);

        MedicalRecordDto.Response response = service.update(7L, request());
        assertEquals("Flu", response.diagnosis());
    }

    @Test
    void doctorCannotEditAnotherDoctorsRecord() {
        existingRecordWrittenBy(doctor);          // written by doctor 5
        TestUsers.loginAs(Role.DOCTOR, 99L);      // a different doctor

        assertThrows(AccessDeniedException.class, () -> service.update(7L, request()));
    }
}
