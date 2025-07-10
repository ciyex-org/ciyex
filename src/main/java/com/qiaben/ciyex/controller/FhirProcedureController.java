package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.FhirProcedureByIdResponseDto;
import com.qiaben.ciyex.dto.FhirProcedureListResponseDto;
import com.qiaben.ciyex.dto.FhirProcedureSearchRequestDto;
import com.qiaben.ciyex.service.FhirProcedureService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/openemr")

public class FhirProcedureController {

    private final FhirProcedureService fhirProcedureService;

    public FhirProcedureController(FhirProcedureService fhirProcedureService) {
        this.fhirProcedureService = fhirProcedureService;
    }

    @GetMapping("/procedure")
    public ResponseEntity<FhirProcedureListResponseDto> getProcedures(FhirProcedureSearchRequestDto req) {
        return ResponseEntity.ok(fhirProcedureService.getProcedures(req));
    }

    @GetMapping("/procedure/{uuid}")
    public ResponseEntity<FhirProcedureByIdResponseDto> getProcedureById(@PathVariable String uuid) {
        return ResponseEntity.ok(fhirProcedureService.getProcedureById(uuid));
    }
}
