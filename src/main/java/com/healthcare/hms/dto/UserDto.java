package com.healthcare.hms.dto;

import com.healthcare.hms.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UserDto {

    private UserDto() {
    }

    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d).{8,100}$";
    public static final String PASSWORD_MESSAGE = "Password must be 8-100 characters and contain a letter and a digit";

    public record CreateRequest(
            @NotBlank(message = "Username is required")
            @Pattern(regexp = "^[a-zA-Z0-9._-]{3,30}$",
                     message = "Username must be 3-30 characters: letters, digits, dot, dash or underscore")
            String username,

            @NotBlank(message = "Password is required")
            @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MESSAGE)
            String password,

            @NotBlank(message = "Full name is required")
            @Size(max = 100, message = "Full name must be at most 100 characters")
            String fullName,

            @NotNull(message = "Role is required")
            Role role,

            /** Required when role is DOCTOR, must be empty otherwise. */
            Long doctorId) {
    }

    public record PasswordRequest(
            @NotBlank(message = "Password is required")
            @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MESSAGE)
            String password) {
    }

    public record EnabledRequest(
            @NotNull(message = "enabled is required")
            Boolean enabled) {
    }

    public record Response(
            Long id,
            String username,
            String fullName,
            Role role,
            boolean enabled,
            Long doctorId,
            String doctorName) {
    }

    /** Returned by /api/auth/me so the UI knows who is logged in. */
    public record Me(String username, String fullName, Role role, Long doctorId) {
    }
}
