package com.qiaben.ciyex.controller.fhir;

import com.qiaben.ciyex.dto.fhir.FhirDocumentReferenceDTO;
import com.qiaben.ciyex.service.fhir.FhirDocumentReferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fhir/DocumentReference")
public class FhirDocumentReferenceController {

    @Autowired
    private FhirDocumentReferenceService documentReferenceService;

    @GetMapping
    public List<FhirDocumentReferenceDTO> getDocumentReferences(
            @RequestParam(required = false) String _id,
            @RequestParam(required = false) String _lastUpdated,
            @RequestParam(required = false) String patient,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String date) {
        return documentReferenceService.getDocumentReferences(_id, _lastUpdated, patient, type, category, date);
    }

    @PostMapping("/$docref")
    public FhirDocumentReferenceDTO generateDocumentReference(@RequestParam String patient, @RequestParam(required = false) String start, @RequestParam(required = false) String end, @RequestParam(required = false) String type) {
        return documentReferenceService.generateDocumentReference(patient, start, end, type);
    }

    @GetMapping("/{uuid}")
    public FhirDocumentReferenceDTO getDocumentReference(@PathVariable String uuid) {
        return documentReferenceService.getDocumentReference(uuid);
    }
}
