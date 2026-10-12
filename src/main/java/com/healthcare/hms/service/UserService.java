package com.healthcare.hms.service;

import com.healthcare.hms.dto.UserDto;
import com.healthcare.hms.entity.AppUser;
import com.healthcare.hms.entity.Doctor;
import com.healthcare.hms.entity.Role;
import com.healthcare.hms.exception.BadRequestException;
import com.healthcare.hms.exception.ConflictException;
import com.healthcare.hms.exception.ResourceNotFoundException;
import com.healthcare.hms.repository.DoctorRepository;
import com.healthcare.hms.repository.UserRepository;
import com.healthcare.hms.security.CurrentUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/** Account management. Only ADMIN can reach it (SecurityConfig). */
@Service
public class UserService {

    private final UserRepository users;
    private final DoctorRepository doctors;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, DoctorRepository doctors, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.doctors = doctors;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserDto.Response> list() {
        return users.findAllByOrderByUsernameAsc().stream().map(UserService::toResponse).toList();
    }

    @Transactional
    public UserDto.Response create(UserDto.CreateRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("That username is already taken.");
        }

        Doctor doctor = null;
        if (request.role() == Role.DOCTOR) {
            if (request.doctorId() == null) {
                throw new BadRequestException("Choose the doctor profile this login belongs to.");
            }
            doctor = doctors.findByIdAndActiveTrue(request.doctorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor not found."));
            if (users.existsByDoctor_Id(doctor.getId())) {
                throw new ConflictException("That doctor already has a login.");
            }
        } else if (request.doctorId() != null) {
            throw new BadRequestException("Only DOCTOR logins can be linked to a doctor profile.");
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        user.setEnabled(true);
        user.setDoctor(doctor);
        return toResponse(users.save(user));
    }

    @Transactional
    public UserDto.Response setEnabled(Long id, boolean enabled) {
        AppUser user = find(id);
        if (!enabled) {
            if (user.getId().equals(CurrentUser.get().getId())) {
                throw new BadRequestException("You cannot disable your own account.");
            }
            if (user.getRole() == Role.ADMIN && user.isEnabled()
                    && users.countByRoleAndEnabledTrue(Role.ADMIN) <= 1) {
                throw new BadRequestException("You cannot disable the last active administrator.");
            }
        }
        user.setEnabled(enabled);
        return toResponse(users.save(user));
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        AppUser user = find(id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        users.save(user);
    }

    private AppUser find(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private static UserDto.Response toResponse(AppUser u) {
        Doctor d = u.getDoctor();
        return new UserDto.Response(u.getId(), u.getUsername(), u.getFullName(), u.getRole(), u.isEnabled(),
                d == null ? null : d.getId(), d == null ? null : d.getFullName());
    }
}
