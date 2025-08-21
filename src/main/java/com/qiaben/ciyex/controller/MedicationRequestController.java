package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.MedicationRequestDto;
import com.qiaben.ciyex.service.MedicationRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medication-requests")
public class MedicationRequestController {

    private final MedicationRequestService service;

    public MedicationRequestController(MedicationRequestService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<MedicationRequestDto> create(@RequestBody MedicationRequestDto dto) {
        MedicationRequestDto createdDto = service.create(dto);
        return ResponseEntity.ok(createdDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicationRequestDto> get(@PathVariable Long id) {
        MedicationRequestDto dto = service.getById(id);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicationRequestDto> update(@PathVariable Long id, @RequestBody MedicationRequestDto dto) {
        MedicationRequestDto updatedDto = service.update(id, dto);
        return ResponseEntity.ok(updatedDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Updated method to fetch medication requests based on optional patientId and encounterId
    @GetMapping
    public ResponseEntity<List<MedicationRequestDto>> getAll(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long encounterId) {

        List<MedicationRequestDto> dtoList = service.getAllByPatientIdOrEncounterId(patientId, encounterId);
        return ResponseEntity.ok(dtoList);
    }
}
