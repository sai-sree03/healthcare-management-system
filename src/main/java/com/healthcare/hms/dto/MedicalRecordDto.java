package com.healthcare.hms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public final class MedicalRecordDto {

    private MedicalRecordDto() {
    }

    /** The doctor is never sent by the client: the server uses the logged-in doctor. */
    public record Request(
            @NotNull(message = "Select a patient")
            Long patientId,

            /** Optional: the visit this record belongs to. */
            Long appointmentId,

            @NotBlank(message = "Diagnosis is required")
            @Size(max = 255, message = "Diagnosis must be at most 255 characters")
            String diagnosis,

            @NotBlank(message = "Treatment is required")
            @Size(max = 2000, message = "Treatment must be at most 2000 characters")
            String treatment,

            @Size(max = 2000, message = "Prescription must be at most 2000 characters")
            String prescription,

            @NotNull(message = "Record date is required")
            @PastOrPresent(message = "Record date cannot be in the future")
            LocalDate recordDate) {
    }

    public record Response(
            Long id,
            Long patientId,
            String patientName,
            Long doctorId,
            String doctorName,
            Long appointmentId,
            String diagnosis,
            String treatment,
            String prescription,
            LocalDate recordDate) {
    }
}
