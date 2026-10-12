package com.healthcare.hms.dto;

import com.healthcare.hms.entity.AppointmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public final class AppointmentDto {

    private AppointmentDto() {
    }

    public record Request(
            @NotNull(message = "Select a patient")
            Long patientId,

            @NotNull(message = "Select a doctor")
            Long doctorId,

            @NotNull(message = "Date is required")
            LocalDate appointmentDate,

            @NotNull(message = "Time is required")
            LocalTime startTime,

            @NotBlank(message = "Reason is required")
            @Size(max = 255, message = "Reason must be at most 255 characters")
            String reason) {
    }

    public record StatusRequest(
            @NotNull(message = "Status is required")
            AppointmentStatus status) {
    }

    public record Response(
            Long id,
            Long patientId,
            String patientName,
            Long doctorId,
            String doctorName,
            LocalDate appointmentDate,
            LocalTime startTime,
            LocalTime endTime,
            String reason,
            AppointmentStatus status) {
    }
}
