package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.FhirPersonByIdResponseDto;
import com.qiaben.ciyex.dto.FhirPersonListResponseDto;
import com.qiaben.ciyex.dto.FhirPersonSearchRequestDto;
import com.qiaben.ciyex.service.FhirPersonService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/openemr")

public class FhirPersonController {

    private final FhirPersonService fhirPersonService;

    public FhirPersonController(FhirPersonService fhirPersonService) {
        this.fhirPersonService = fhirPersonService;
    }

    @GetMapping("/person")
    public ResponseEntity<FhirPersonListResponseDto> searchPersons(FhirPersonSearchRequestDto req) {
        return ResponseEntity.ok(fhirPersonService.getPersons(req));
    }

    @GetMapping("/person/{uuid}")
    public ResponseEntity<FhirPersonByIdResponseDto> getPersonByUuid(@PathVariable String uuid) {
        return ResponseEntity.ok(fhirPersonService.getPersonById(uuid));
    }
}
