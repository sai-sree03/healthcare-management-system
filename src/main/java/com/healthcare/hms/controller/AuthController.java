package com.healthcare.hms.controller;

import com.healthcare.hms.dto.UserDto;
import com.healthcare.hms.security.CurrentUser;
import com.healthcare.hms.security.HmsUserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Login and logout themselves are handled by Spring Security (see SecurityConfig). */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    public UserDto.Me me() {
        HmsUserDetails user = CurrentUser.get();
        return new UserDto.Me(user.getUsername(), user.getFullName(), user.getRole(), user.getDoctorId());
    }
}
