package com.healthcare.hms.security;

import com.healthcare.hms.entity.Role;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** The logged-in user as Spring Security sees it, including role and (for doctors) their doctor profile id. */
public class HmsUserDetails implements UserDetails, CredentialsContainer {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String username;
    private String password;
    private final String fullName;
    private final Role role;
    private final Long doctorId;
    private final boolean enabled;

    public HmsUserDetails(Long id, String username, String password, String fullName,
                          Role role, Long doctorId, boolean enabled) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.doctorId = doctorId;
        this.enabled = enabled;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
    /** Null unless the user is a DOCTOR linked to a doctor profile. */
    public Long getDoctorId() { return doctorId; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return username; }
    @Override public boolean isEnabled() { return enabled; }

    /** Spring calls this after login so the password hash is not kept in the session. */
    @Override
    public void eraseCredentials() {
        this.password = null;
    }
}
