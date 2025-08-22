package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.InsuranceCompanyDto;
import com.qiaben.ciyex.service.InsuranceCompanyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/insurance-companies")
public class InsuranceCompanyController {

    private final InsuranceCompanyService service;

    public InsuranceCompanyController(InsuranceCompanyService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<InsuranceCompanyDto> create(@RequestBody InsuranceCompanyDto dto) {
        InsuranceCompanyDto createdDto = service.create(dto);
        return ResponseEntity.ok(createdDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InsuranceCompanyDto> get(@PathVariable Long id) {
        InsuranceCompanyDto dto = service.getById(id);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InsuranceCompanyDto> update(@PathVariable Long id, @RequestBody InsuranceCompanyDto dto) {
        InsuranceCompanyDto updatedDto = service.update(id, dto);
        return ResponseEntity.ok(updatedDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<InsuranceCompanyDto>> getAll() {
        List<InsuranceCompanyDto> dtoList = service.getAll();
        return ResponseEntity.ok(dtoList);
    }
}
