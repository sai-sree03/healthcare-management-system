package com.healthcare.hms.controller;

import com.healthcare.hms.dto.DoctorDto;
import com.healthcare.hms.dto.OptionDto;
import com.healthcare.hms.dto.PageResponse;
import com.healthcare.hms.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService service;

    public DoctorController(DoctorService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<DoctorDto.Response> list(@RequestParam(defaultValue = "") String search,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        return service.list(search, page, size);
    }

    @GetMapping("/options")
    public List<OptionDto> options() {
        return service.options();
    }

    @GetMapping("/{id}")
    public DoctorDto.Response get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<DoctorDto.Response> create(@Valid @RequestBody DoctorDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public DoctorDto.Response update(@PathVariable Long id, @Valid @RequestBody DoctorDto.Request request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
