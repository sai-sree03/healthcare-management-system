package com.healthcare.hms.controller;

import com.healthcare.hms.dto.OptionDto;
import com.healthcare.hms.dto.PageResponse;
import com.healthcare.hms.dto.PatientDto;
import com.healthcare.hms.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService service;

    public PatientController(PatientService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<PatientDto.Response> list(@RequestParam(defaultValue = "") String search,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return service.list(search, page, size);
    }

    @GetMapping("/options")
    public List<OptionDto> options(@RequestParam(defaultValue = "") String search) {
        return service.options(search);
    }

    @GetMapping("/{id}")
    public PatientDto.Response get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<PatientDto.Response> create(@Valid @RequestBody PatientDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public PatientDto.Response update(@PathVariable Long id, @Valid @RequestBody PatientDto.Request request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
