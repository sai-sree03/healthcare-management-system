package com.healthcare.hms.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/** Small helper so services can ask "who is calling?" without touching Spring Security directly. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<HmsUserDetails> find() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof HmsUserDetails user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public static HmsUserDetails get() {
        return find().orElseThrow(() -> new AccessDeniedException("You must be logged in."));
    }
}
