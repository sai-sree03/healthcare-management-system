package com.healthcare.hms.service;

import com.healthcare.hms.dto.DashboardDto;
import com.healthcare.hms.entity.AppointmentStatus;
import com.healthcare.hms.entity.Role;
import com.healthcare.hms.repository.AppointmentRepository;
import com.healthcare.hms.repository.DoctorRepository;
import com.healthcare.hms.repository.MedicalRecordRepository;
import com.healthcare.hms.repository.PatientRepository;
import com.healthcare.hms.security.CurrentUser;
import com.healthcare.hms.security.HmsUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Service
public class DashboardService {

    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;
    private final MedicalRecordRepository records;
    private final Clock clock;

    public DashboardService(PatientRepository patients, DoctorRepository doctors,
                            AppointmentRepository appointments, MedicalRecordRepository records, Clock clock) {
        this.patients = patients;
        this.doctors = doctors;
        this.appointments = appointments;
        this.records = records;
        this.clock = clock;
    }

    /** Numbers are role-aware: doctors see their own appointment counts; receptionists get no clinical counts. */
    @Transactional(readOnly = true)
    public DashboardDto.Stats stats() {
        HmsUserDetails user = CurrentUser.get();
        LocalDate today = LocalDate.now(clock);

        long todayCount;
        long scheduledCount;
        if (user.getRole() == Role.DOCTOR) {
            Long doctorId = user.getDoctorId();
            if (doctorId == null) {
                // login not linked to a doctor profile yet: show zeros instead of failing the whole dashboard
                todayCount = 0;
                scheduledCount = 0;
            } else {
                todayCount = appointments.countByAppointmentDateAndStatusNotAndDoctor_Id(today, AppointmentStatus.CANCELLED, doctorId);
                scheduledCount = appointments.countByStatusAndDoctor_Id(AppointmentStatus.SCHEDULED, doctorId);
            }
        } else {
            todayCount = appointments.countByAppointmentDateAndStatusNot(today, AppointmentStatus.CANCELLED);
            scheduledCount = appointments.countByStatus(AppointmentStatus.SCHEDULED);
        }

        boolean canSeeRecords = user.getRole() == Role.ADMIN || user.getRole() == Role.DOCTOR;
        Long recordCount = canSeeRecords ? records.countByActiveTrue() : null;

        return new DashboardDto.Stats(patients.countByActiveTrue(), doctors.countByActiveTrue(),
                todayCount, scheduledCount, recordCount);
    }
}
