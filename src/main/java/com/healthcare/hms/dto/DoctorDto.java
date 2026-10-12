package com.healthcare.hms.dto;

import com.healthcare.hms.entity.Doctor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public final class DoctorDto {

    private DoctorDto() {
    }

    public record Request(
            @NotBlank(message = "Full name is required")
            @Size(max = 100, message = "Full name must be at most 100 characters")
            String fullName,

            @NotBlank(message = "Email is required")
            @Email(message = "Enter a valid email address")
            @Size(max = 120, message = "Email must be at most 120 characters")
            String email,

            @NotBlank(message = "Phone is required")
            @Pattern(regexp = PatientDto.PHONE_REGEX, message = PatientDto.PHONE_MESSAGE)
            String phone,

            @NotBlank(message = "Specialization is required")
            @Size(max = 100, message = "Specialization must be at most 100 characters")
            String specialization,

            @Size(max = 150, message = "Qualification must be at most 150 characters")
            String qualification,

            @NotNull(message = "Experience is required")
            @Min(value = 0, message = "Experience cannot be negative")
            @Max(value = 60, message = "Experience cannot be more than 60 years")
            Integer experienceYears,

            @NotNull(message = "Available-from time is required")
            LocalTime availableFrom,

            @NotNull(message = "Available-to time is required")
            LocalTime availableTo) {
    }

    public record Response(
            Long id,
            String fullName,
            String email,
            String phone,
            String specialization,
            String qualification,
            int experienceYears,
            LocalTime availableFrom,
            LocalTime availableTo) {

        public static Response from(Doctor d) {
            return new Response(d.getId(), d.getFullName(), d.getEmail(), d.getPhone(),
                    d.getSpecialization(), d.getQualification(), d.getExperienceYears(),
                    d.getAvailableFrom(), d.getAvailableTo());
        }
    }
}
