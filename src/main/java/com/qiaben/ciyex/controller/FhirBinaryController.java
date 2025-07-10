package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.service.FhirBinaryService;
import com.qiaben.ciyex.dto.FhirBinaryResponseDTO;
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

