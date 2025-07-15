package com.qiaben.ciyex.controller.fhir;

import com.qiaben.ciyex.dto.fhir.FhirPersonByIdResponseDto;
import com.qiaben.ciyex.dto.fhir.FhirPersonListResponseDto;
import com.qiaben.ciyex.dto.fhir.FhirPersonSearchRequestDto;
import com.qiaben.ciyex.service.fhir.FhirPersonService;
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
    public ResponseEntity<FhirPersonListResponseDto> searchPersons(FhirPersonSearchRequestDto req) throws Exception {
        return ResponseEntity.ok(fhirPersonService.getPersons(req));
    }

    @GetMapping("/person/{uuid}")
    public ResponseEntity<FhirPersonByIdResponseDto> getPersonByUuid(@PathVariable String uuid) throws Exception {
        return ResponseEntity.ok(fhirPersonService.getPersonById(uuid));
    }
}
