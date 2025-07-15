package com.qiaben.ciyex.controller.fhir;

import com.qiaben.ciyex.service.fhir.FhirOperationDefinitionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fhir/OperationDefinition")
public class FhirOperationDefinitionController {

    private final FhirOperationDefinitionService fhirOperationDefinitionService;

    public FhirOperationDefinitionController(FhirOperationDefinitionService fhirOperationDefinitionService) {
        this.fhirOperationDefinitionService = fhirOperationDefinitionService;
    }

    @GetMapping
    public ResponseEntity<Object> getAllOperationDefinitions() {
        return fhirOperationDefinitionService.getAllOperationDefinitions();
    }

    @GetMapping("/{operation}")
    public ResponseEntity<Object> getOperationDefinitionByName(@PathVariable String operation) {
        return fhirOperationDefinitionService.getOperationDefinitionByName(operation);
    }
}