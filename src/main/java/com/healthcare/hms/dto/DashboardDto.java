package com.healthcare.hms.dto;

public final class DashboardDto {

    private DashboardDto() {
    }

    /** medicalRecords is null for roles that may not see clinical data. */
    public record Stats(long patients, long doctors, long todayAppointments,
                        long scheduledAppointments, Long medicalRecords) {
    }
}
