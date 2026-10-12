package com.healthcare.hms.dto;

import com.healthcare.hms.entity.Gender;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private PatientDto.Request patient(String name, LocalDate dob, String phone, String email, String blood) {
        return new PatientDto.Request(name, dob, Gender.MALE, phone, email, null, blood, null, null);
    }

    private boolean hasViolationOn(Set<? extends ConstraintViolation<?>> violations, String field) {
        return violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals(field));
    }

    @Test
    void validPatientPasses() {
        var violations = validator.validate(
                patient("Ravi Kumar", LocalDate.of(1990, 5, 14), "9876543210", "ravi@example.com", "O+"));
        assertTrue(violations.isEmpty());
    }

    @Test
    void badPatientFieldsAreRejected() {
        var violations = validator.validate(
                patient("  ", LocalDate.now().plusDays(1), "abc", "not-an-email", "Z+"));
        assertTrue(hasViolationOn(violations, "fullName"));
        assertTrue(hasViolationOn(violations, "dateOfBirth"));
        assertTrue(hasViolationOn(violations, "phone"));
        assertTrue(hasViolationOn(violations, "email"));
        assertTrue(hasViolationOn(violations, "bloodGroup"));
    }

    @Test
    void passwordPolicyIsEnforced() {
        assertFalse(validator.validate(new UserDto.PasswordRequest("short1")).isEmpty());          // too short
        assertFalse(validator.validate(new UserDto.PasswordRequest("onlyletters")).isEmpty());     // no digit
        assertTrue(validator.validate(new UserDto.PasswordRequest("Str0ngPass")).isEmpty());
    }
}
