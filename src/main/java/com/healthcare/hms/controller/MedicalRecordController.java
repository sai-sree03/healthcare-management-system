package com.healthcare.hms.controller;

import com.healthcare.hms.dto.MedicalRecordDto;
import com.healthcare.hms.dto.PageResponse;
import com.healthcare.hms.service.MedicalRecordService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/medical-records")
public class MedicalRecordController {

    private final MedicalRecordService service;

    public MedicalRecordController(MedicalRecordService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<MedicalRecordDto.Response> list(@RequestParam(required = false) Long patientId,
                                                        @RequestParam(defaultValue = "") String search,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return service.list(patientId, search, page, size);
    }

    @GetMapping("/{id}")
    public MedicalRecordDto.Response get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<MedicalRecordDto.Response> create(@Valid @RequestBody MedicalRecordDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public MedicalRecordDto.Response update(@PathVariable Long id,
                                            @Valid @RequestBody MedicalRecordDto.Request request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
