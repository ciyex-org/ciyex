
package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.InsuranceCompanyDto;
import com.qiaben.ciyex.entity.InsuranceStatus;
import com.qiaben.ciyex.service.InsuranceCompanyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/insurance-companies")
public class InsuranceCompanyController {

    private final InsuranceCompanyService service;

    public InsuranceCompanyController(InsuranceCompanyService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<InsuranceCompanyDto> create(@Valid @RequestBody InsuranceCompanyDto dto) {
        InsuranceCompanyDto createdDto = service.create(dto);
        return ResponseEntity.ok(createdDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InsuranceCompanyDto> get(@PathVariable String id) {
        InsuranceCompanyDto dto = service.getById(id);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InsuranceCompanyDto> update(@PathVariable String id, @Valid @RequestBody InsuranceCompanyDto dto) {
        InsuranceCompanyDto updatedDto = service.update(id, dto);
        return ResponseEntity.ok(updatedDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable String id) {
        service.delete(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Insurance company deleted successfully");
        response.put("id", id);
        return ResponseEntity.ok(response);
    }

    // 🔹 New endpoints for status toggle
    @PostMapping("/{id}/archive")
    public ResponseEntity<InsuranceCompanyDto> archive(@PathVariable String id) {
        return ResponseEntity.ok(service.updateStatus(id, "ARCHIVED"));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<InsuranceCompanyDto> activate(@PathVariable String id) {
        return ResponseEntity.ok(service.updateStatus(id, "ACTIVE"));
    }

    @GetMapping
    public ResponseEntity<List<InsuranceCompanyDto>> getAll() {
        List<InsuranceCompanyDto> dtoList = service.getAll();
        return ResponseEntity.ok(dtoList);
    }
}
