package org.ciyex.ehr.lab.controller;

import lombok.RequiredArgsConstructor;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.lab.dto.LabOrderSetDto;
import org.ciyex.ehr.lab.service.LabOrderSetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@PreAuthorize("hasAuthority('SCOPE_user/DiagnosticReport.read')")
@RestController
@RequestMapping("/api/lab-order-sets")
@RequiredArgsConstructor
public class LabOrderSetController {

    private final LabOrderSetService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LabOrderSetDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok("Order sets retrieved", service.getAll()));
    }
}
