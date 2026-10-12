package com.healthcare.hms.dto;

import com.healthcare.hms.entity.Gender;
import com.healthcare.hms.entity.Patient;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.Period;

public final class PatientDto {

    private PatientDto() {
    }

    public static final String PHONE_REGEX = "^\\+?[0-9 \\-]{7,18}$";
    public static final String PHONE_MESSAGE = "Enter a valid phone number (7-18 digits, may start with +)";

    public record Request(
            @NotBlank(message = "Full name is required")
            @Size(max = 100, message = "Full name must be at most 100 characters")
            String fullName,

            @NotNull(message = "Date of birth is required")
            @Past(message = "Date of birth must be in the past")
            LocalDate dateOfBirth,

            @NotNull(message = "Gender is required")
            Gender gender,

            @NotBlank(message = "Phone is required")
            @Pattern(regexp = PHONE_REGEX, message = PHONE_MESSAGE)
            String phone,

            @NotBlank(message = "Email is required")
            @Email(message = "Enter a valid email address")
            @Size(max = 120, message = "Email must be at most 120 characters")
            String email,

            @Size(max = 255, message = "Address must be at most 255 characters")
            String address,

            @Pattern(regexp = "^(A|B|AB|O)[+-]$", message = "Blood group must look like A+, O-, AB+")
            String bloodGroup,

            @Size(max = 100, message = "Emergency contact name must be at most 100 characters")
            String emergencyContactName,

            @Pattern(regexp = PHONE_REGEX, message = PHONE_MESSAGE)
            String emergencyContactPhone) {
    }

    public record Response(
            Long id,
            String fullName,
            LocalDate dateOfBirth,
            int age,
            Gender gender,
            String phone,
            String email,
            String address,
            String bloodGroup,
            String emergencyContactName,
            String emergencyContactPhone) {

        public static Response from(Patient p) {
            int age = Period.between(p.getDateOfBirth(), LocalDate.now()).getYears();
            return new Response(p.getId(), p.getFullName(), p.getDateOfBirth(), age, p.getGender(),
                    p.getPhone(), p.getEmail(), p.getAddress(), p.getBloodGroup(),
                    p.getEmergencyContactName(), p.getEmergencyContactPhone());
        }
    }
}
