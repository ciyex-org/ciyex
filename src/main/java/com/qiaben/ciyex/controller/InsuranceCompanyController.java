package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.InsuranceCompanyDto;
import com.qiaben.ciyex.dto.InsuranceStatus;
import com.qiaben.ciyex.service.InsuranceCompanyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<?> create(@Valid @RequestBody InsuranceCompanyDto dto) {
        try {
            InsuranceCompanyDto createdDto = service.create(dto);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Insurance company created successfully");
            response.put("data", createdDto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to create insurance company: " + e.getMessage());
            error.put("data", null);
            return ResponseEntity.ok(error);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable("id") String id) {
        InsuranceCompanyDto dto = service.getById(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Insurance company retrieved successfully");
        response.put("data", dto);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") String id, @Valid @RequestBody InsuranceCompanyDto dto) {
        InsuranceCompanyDto updatedDto = service.update(id, dto);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Insurance company updated successfully");
        response.put("data", updatedDto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") String id) {
        service.delete(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Insurance company deleted successfully");
        return ResponseEntity.ok(response);
    }

    // 🔹 New endpoints for status toggle
    @PostMapping("/{id}/archive")
    public ResponseEntity<?> archive(@PathVariable("id") String id) {
        InsuranceCompanyDto dto = service.updateStatus(id, "ARCHIVED");
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Insurance company archived successfully");
        response.put("data", dto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<?> activate(@PathVariable("id") String id) {
        InsuranceCompanyDto dto = service.updateStatus(id, "ACTIVE");
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Insurance company activated successfully");
        response.put("data", dto);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        try {
            List<InsuranceCompanyDto> dtoList = service.getAll();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Insurance companies retrieved successfully");
            response.put("data", dtoList);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to retrieve insurance companies: " + e.getMessage());
            error.put("data", null);
            return ResponseEntity.ok(error);
        }
    }
}
