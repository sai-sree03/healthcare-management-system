package com.healthcare.hms.security;

import com.healthcare.hms.entity.AppUser;
import com.healthcare.hms.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HmsUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public HmsUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = users.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
        Long doctorId = user.getDoctor() == null ? null : user.getDoctor().getId();
        return new HmsUserDetails(user.getId(), user.getUsername(), user.getPasswordHash(),
                user.getFullName(), user.getRole(), doctorId, user.isEnabled());
    }
}
