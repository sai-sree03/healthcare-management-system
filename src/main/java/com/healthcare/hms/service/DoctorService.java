package com.healthcare.hms.service;

import com.healthcare.hms.dto.DoctorDto;
import com.healthcare.hms.dto.OptionDto;
import com.healthcare.hms.dto.PageResponse;
import com.healthcare.hms.entity.AppointmentStatus;
import com.healthcare.hms.entity.Doctor;
import com.healthcare.hms.exception.BadRequestException;
import com.healthcare.hms.exception.ConflictException;
import com.healthcare.hms.exception.ResourceNotFoundException;
import com.healthcare.hms.repository.AppointmentRepository;
import com.healthcare.hms.repository.DoctorRepository;
import com.healthcare.hms.util.Paging;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class DoctorService {

    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;

    public DoctorService(DoctorRepository doctors, AppointmentRepository appointments) {
        this.doctors = doctors;
        this.appointments = appointments;
    }

    @Transactional(readOnly = true)
    public PageResponse<DoctorDto.Response> list(String search, int page, int size) {
        var pageable = Paging.of(page, size, Sort.by("fullName").ascending());
        return PageResponse.from(doctors.search(Paging.text(search), pageable), DoctorDto.Response::from);
    }

    @Transactional(readOnly = true)
    public List<OptionDto> options() {
        return doctors.findByActiveTrueOrderByFullNameAsc().stream()
                .map(d -> new OptionDto(d.getId(), d.getFullName() + " - " + d.getSpecialization()))
                .toList();
    }

    @Transactional(readOnly = true)
    public DoctorDto.Response get(Long id) {
        return DoctorDto.Response.from(find(id));
    }

    @Transactional
    public DoctorDto.Response create(DoctorDto.Request request) {
        validateAvailability(request);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (doctors.existsByEmailIgnoreCaseAndActiveTrue(email)) {
            throw new ConflictException("A doctor with this email already exists.");
        }
        Doctor doctor = new Doctor();
        apply(doctor, request, email);
        return DoctorDto.Response.from(doctors.save(doctor));
    }

    @Transactional
    public DoctorDto.Response update(Long id, DoctorDto.Request request) {
        validateAvailability(request);
        Doctor doctor = find(id);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (doctors.existsByEmailIgnoreCaseAndActiveTrueAndIdNot(email, id)) {
            throw new ConflictException("Another doctor already uses this email.");
        }
        apply(doctor, request, email);
        return DoctorDto.Response.from(doctors.save(doctor));
    }

    @Transactional
    public void delete(Long id) {
        Doctor doctor = find(id);
        if (appointments.existsByDoctor_IdAndStatus(id, AppointmentStatus.SCHEDULED)) {
            throw new ConflictException("This doctor still has scheduled appointments. Cancel or complete them first.");
        }
        doctor.setActive(false);
        doctors.save(doctor);
    }

    public Doctor find(Long id) {
        return doctors.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found."));
    }

    private static void validateAvailability(DoctorDto.Request r) {
        if (!r.availableFrom().isBefore(r.availableTo())) {
            throw new BadRequestException("'Available from' must be earlier than 'Available to'.");
        }
    }

    private static void apply(Doctor d, DoctorDto.Request r, String email) {
        d.setFullName(r.fullName().trim());
        d.setEmail(email);
        d.setPhone(r.phone().trim());
        d.setSpecialization(r.specialization().trim());
        d.setQualification(PatientService.blankToNull(r.qualification()));
        d.setExperienceYears(r.experienceYears());
        d.setAvailableFrom(r.availableFrom());
        d.setAvailableTo(r.availableTo());
    }
}
