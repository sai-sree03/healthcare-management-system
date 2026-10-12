package com.healthcare.hms.controller;

import com.healthcare.hms.dto.UserDto;
import com.healthcare.hms.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Administrator-only account management. */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public List<UserDto.Response> list() {
        return service.list();
    }

    @PostMapping
    public ResponseEntity<UserDto.Response> create(@Valid @RequestBody UserDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PatchMapping("/{id}/enabled")
    public UserDto.Response setEnabled(@PathVariable Long id, @Valid @RequestBody UserDto.EnabledRequest request) {
        return service.setEnabled(id, request.enabled());
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody UserDto.PasswordRequest request) {
        service.resetPassword(id, request.password());
        return ResponseEntity.noContent().build();
    }
}
