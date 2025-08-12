package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ReferralPracticeDto;
import com.qiaben.ciyex.service.ReferralPracticeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/referral-practices")
public class ReferralPracticeController {

    private final ReferralPracticeService service;

    public ReferralPracticeController(ReferralPracticeService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ReferralPracticeDto> create(@RequestBody ReferralPracticeDto dto) {
        ReferralPracticeDto createdDto = service.create(dto);
        return ResponseEntity.ok(createdDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReferralPracticeDto> get(@PathVariable Long id) {
        ReferralPracticeDto dto = service.getById(id);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReferralPracticeDto> update(@PathVariable Long id, @RequestBody ReferralPracticeDto dto) {
        ReferralPracticeDto updatedDto = service.update(id, dto);
        return ResponseEntity.ok(updatedDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<ReferralPracticeDto>> getAll() {
        List<ReferralPracticeDto> dtoList = service.getAll();
        return ResponseEntity.ok(dtoList);
    }
}
