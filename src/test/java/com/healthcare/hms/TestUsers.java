package com.healthcare.hms;

import com.healthcare.hms.entity.Role;
import com.healthcare.hms.security.HmsUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** Test helper: pretend a user with a given role is logged in. */
public final class TestUsers {

    private TestUsers() {
    }

    public static void loginAs(Role role, Long doctorId) {
        HmsUserDetails user = new HmsUserDetails(1L, "tester", "x", "Test User", role, doctorId, true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    public static void logout() {
        SecurityContextHolder.clearContext();
    }
}
