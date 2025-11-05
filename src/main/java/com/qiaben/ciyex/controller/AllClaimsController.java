package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.PatientClaimDto;
import com.qiaben.ciyex.service.PatientBillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/all-claims")
public class AllClaimsController {
    private final PatientBillingService service;

    @GetMapping
    public ResponseEntity<List<PatientClaimDto>> listAllClaims() {
        var data = service.listAllClaims();
        return ResponseEntity.ok(data);
    }
}

