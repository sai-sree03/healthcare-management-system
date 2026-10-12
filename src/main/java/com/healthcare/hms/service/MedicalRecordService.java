package com.healthcare.hms.service;

import com.healthcare.hms.dto.MedicalRecordDto;
import com.healthcare.hms.dto.PageResponse;
import com.healthcare.hms.entity.Appointment;
import com.healthcare.hms.entity.Doctor;
import com.healthcare.hms.entity.MedicalRecord;
import com.healthcare.hms.entity.Patient;
import com.healthcare.hms.exception.BadRequestException;
import com.healthcare.hms.exception.ResourceNotFoundException;
import com.healthcare.hms.repository.AppointmentRepository;
import com.healthcare.hms.repository.DoctorRepository;
import com.healthcare.hms.repository.MedicalRecordRepository;
import com.healthcare.hms.repository.PatientRepository;
import com.healthcare.hms.security.CurrentUser;
import com.healthcare.hms.security.HmsUserDetails;
import com.healthcare.hms.util.Paging;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Clinical data. URL-level rules (SecurityConfig) already limit this to ADMIN (read, delete)
 * and DOCTOR (read, create, edit). The rules here add ownership:
 *  - the author of a record is always the logged-in doctor, never something the client sends
 *  - a doctor can only edit records they wrote themselves
 */
@Service
public class MedicalRecordService {

    private final MedicalRecordRepository records;
    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;

    public MedicalRecordService(MedicalRecordRepository records, PatientRepository patients,
                                DoctorRepository doctors, AppointmentRepository appointments) {
        this.records = records;
        this.patients = patients;
        this.doctors = doctors;
        this.appointments = appointments;
    }

    @Transactional(readOnly = true)
    public PageResponse<MedicalRecordDto.Response> list(Long patientId, String search, int page, int size) {
        var pageable = Paging.of(page, size,
                Sort.by(Sort.Order.desc("recordDate"), Sort.Order.desc("id")));
        return PageResponse.from(records.search(patientId, Paging.text(search), pageable), this::toResponse);
    }

    @Transactional(readOnly = true)
    public MedicalRecordDto.Response get(Long id) {
        return toResponse(load(id));
    }

    @Transactional
    public MedicalRecordDto.Response create(MedicalRecordDto.Request request) {
        HmsUserDetails user = CurrentUser.get();
        Long doctorId = AppointmentService.requireDoctorId(user);

        Doctor doctor = doctors.findByIdAndActiveTrue(doctorId)
                .orElseThrow(() -> new AccessDeniedException("Your doctor profile is no longer active."));
        Patient patient = patients.findByIdAndActiveTrue(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found."));

        MedicalRecord record = new MedicalRecord();
        record.setPatient(patient);
        record.setDoctor(doctor);
        record.setAppointment(resolveAppointment(request.appointmentId(), patient, doctor));
        apply(record, request);
        return toResponse(records.save(record));
    }

    @Transactional
    public MedicalRecordDto.Response update(Long id, MedicalRecordDto.Request request) {
        HmsUserDetails user = CurrentUser.get();
        Long doctorId = AppointmentService.requireDoctorId(user);

        MedicalRecord record = load(id);
        if (!doctorId.equals(record.getDoctor().getId())) {
            throw new AccessDeniedException("You can only edit medical records that you wrote.");
        }
        // the patient of an existing record cannot be switched to someone else
        if (!record.getPatient().getId().equals(request.patientId())) {
            throw new BadRequestException("The patient of an existing record cannot be changed.");
        }
        record.setAppointment(resolveAppointment(request.appointmentId(), record.getPatient(), record.getDoctor()));
        apply(record, request);
        return toResponse(records.save(record));
    }

    /** Soft delete (ADMIN only, enforced in SecurityConfig): the row stays in the database. */
    @Transactional
    public void delete(Long id) {
        MedicalRecord record = load(id);
        record.setActive(false);
        records.save(record);
    }

    private Appointment resolveAppointment(Long appointmentId, Patient patient, Doctor doctor) {
        if (appointmentId == null) {
            return null;
        }
        Appointment appointment = appointments.findWithDetailsById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found."));
        if (!appointment.getPatient().getId().equals(patient.getId())
                || !appointment.getDoctor().getId().equals(doctor.getId())) {
            throw new BadRequestException("That appointment does not belong to this patient and doctor.");
        }
        return appointment;
    }

    private static void apply(MedicalRecord record, MedicalRecordDto.Request r) {
        record.setDiagnosis(r.diagnosis().trim());
        record.setTreatment(r.treatment().trim());
        record.setPrescription(PatientService.blankToNull(r.prescription()));
        record.setRecordDate(r.recordDate());
    }

    private MedicalRecord load(Long id) {
        return records.findWithDetailsByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medical record not found."));
    }

    private MedicalRecordDto.Response toResponse(MedicalRecord r) {
        return new MedicalRecordDto.Response(
                r.getId(),
                r.getPatient().getId(), r.getPatient().getFullName(),
                r.getDoctor().getId(), r.getDoctor().getFullName(),
                r.getAppointment() == null ? null : r.getAppointment().getId(),
                r.getDiagnosis(), r.getTreatment(), r.getPrescription(), r.getRecordDate());
    }
}
