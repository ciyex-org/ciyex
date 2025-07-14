package com.qiaben.ciyex.controller.fhir;

import com.qiaben.ciyex.service.fhir.FhirBinaryService;
import com.qiaben.ciyex.dto.fhir.FhirBinaryResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fhir")
public class FhirBinaryController {

    @Autowired
    private FhirBinaryService binaryService;

    @GetMapping("/Binary/{id}")
    public FhirBinaryResponseDTO getBinaryDocument(@PathVariable String id) {
        return binaryService.getBinaryDocument(id);
    }
}

