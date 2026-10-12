package com.healthcare.hms.service;

import com.healthcare.hms.dto.OptionDto;
import com.healthcare.hms.dto.PageResponse;
import com.healthcare.hms.dto.PatientDto;
import com.healthcare.hms.entity.AppointmentStatus;
import com.healthcare.hms.entity.Patient;
import com.healthcare.hms.exception.ConflictException;
import com.healthcare.hms.exception.ResourceNotFoundException;
import com.healthcare.hms.repository.AppointmentRepository;
import com.healthcare.hms.repository.PatientRepository;
import com.healthcare.hms.util.Paging;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class PatientService {

    private final PatientRepository patients;
    private final AppointmentRepository appointments;

    public PatientService(PatientRepository patients, AppointmentRepository appointments) {
        this.patients = patients;
        this.appointments = appointments;
    }

    @Transactional(readOnly = true)
    public PageResponse<PatientDto.Response> list(String search, int page, int size) {
        var pageable = Paging.of(page, size, Sort.by("fullName").ascending());
        return PageResponse.from(patients.search(Paging.text(search), pageable), PatientDto.Response::from);
    }

    /** Top matches for the "type a patient name" boxes. */
    @Transactional(readOnly = true)
    public List<OptionDto> options(String search) {
        var pageable = Paging.of(0, 20, Sort.by("fullName").ascending());
        return patients.search(Paging.text(search), pageable).getContent().stream()
                .map(p -> new OptionDto(p.getId(), p.getFullName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PatientDto.Response get(Long id) {
        return PatientDto.Response.from(find(id));
    }

    @Transactional
    public PatientDto.Response create(PatientDto.Request request) {
        String email = normalizeEmail(request.email());
        if (patients.existsByEmailIgnoreCaseAndActiveTrue(email)) {
            throw new ConflictException("A patient with this email already exists.");
        }
        Patient patient = new Patient();
        apply(patient, request, email);
        return PatientDto.Response.from(patients.save(patient));
    }

    @Transactional
    public PatientDto.Response update(Long id, PatientDto.Request request) {
        Patient patient = find(id);
        String email = normalizeEmail(request.email());
        if (patients.existsByEmailIgnoreCaseAndActiveTrueAndIdNot(email, id)) {
            throw new ConflictException("Another patient already uses this email.");
        }
        apply(patient, request, email);
        return PatientDto.Response.from(patients.save(patient));
    }

    /** Soft delete: the patient disappears from lists but their history is kept. */
    @Transactional
    public void delete(Long id) {
        Patient patient = find(id);
        if (appointments.existsByPatient_IdAndStatus(id, AppointmentStatus.SCHEDULED)) {
            throw new ConflictException("This patient still has scheduled appointments. Cancel them first.");
        }
        patient.setActive(false);
        patients.save(patient);
    }

    /** Used by other services that need a patient to exist and be active. */
    public Patient find(Long id) {
        return patients.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found."));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static void apply(Patient p, PatientDto.Request r, String email) {
        p.setFullName(r.fullName().trim());
        p.setDateOfBirth(r.dateOfBirth());
        p.setGender(r.gender());
        p.setPhone(r.phone().trim());
        p.setEmail(email);
        p.setAddress(blankToNull(r.address()));
        p.setBloodGroup(blankToNull(r.bloodGroup()));
        p.setEmergencyContactName(blankToNull(r.emergencyContactName()));
        p.setEmergencyContactPhone(blankToNull(r.emergencyContactPhone()));
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
