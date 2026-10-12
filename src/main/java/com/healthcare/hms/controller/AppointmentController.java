package com.healthcare.hms.controller;

import com.healthcare.hms.dto.AppointmentDto;
import com.healthcare.hms.dto.PageResponse;
import com.healthcare.hms.entity.AppointmentStatus;
import com.healthcare.hms.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService service;

    public AppointmentController(AppointmentService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<AppointmentDto.Response> list(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return service.list(doctorId, patientId, date, status, page, size);
    }

    @GetMapping("/{id}")
    public AppointmentDto.Response get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<AppointmentDto.Response> create(@Valid @RequestBody AppointmentDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public AppointmentDto.Response update(@PathVariable Long id, @Valid @RequestBody AppointmentDto.Request request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public AppointmentDto.Response changeStatus(@PathVariable Long id,
                                                @Valid @RequestBody AppointmentDto.StatusRequest request) {
        return service.changeStatus(id, request.status());
    }
}
