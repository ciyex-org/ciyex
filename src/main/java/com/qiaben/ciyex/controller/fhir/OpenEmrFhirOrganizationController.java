package com.qiaben.ciyex.controller.fhir;

import com.qiaben.ciyex.dto.fhir.OrganizationRequestDTO;  // Use OrganizationRequestDTO for POST requests
import com.qiaben.ciyex.dto.fhir.OrganizationResponseDTO;
import com.qiaben.ciyex.service.fhir.OpenEmrFhirOrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fhir/Organization")
public class OpenEmrFhirOrganizationController {

    private final OpenEmrFhirOrganizationService organizationService;

    @Autowired
    public OpenEmrFhirOrganizationController(OpenEmrFhirOrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    public List<OrganizationResponseDTO> getOrganizations(
            @RequestParam(required = false) String _id,
            @RequestParam(required = false) String _lastUpdated,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String telecom,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String addressCity,
            @RequestParam(required = false) String addressPostalCode,
            @RequestParam(required = false) String addressState) {
        return organizationService.getOrganizations(_id, _lastUpdated, name, email, phone, telecom, address, addressCity, addressPostalCode, addressState);
    }

    @PostMapping
    public ResponseEntity<OrganizationResponseDTO> postOrganization(@RequestBody OrganizationRequestDTO organizationRequestDTO) {
        // Call the service method to add the organization using the request DTO
        OrganizationResponseDTO createdOrganization = organizationService.postOrganization(organizationRequestDTO);
        return new ResponseEntity<>(createdOrganization, HttpStatus.CREATED);
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<OrganizationResponseDTO> getOrganizationByUuid(@PathVariable String uuid) {
        OrganizationResponseDTO organization = organizationService.getOrganizationByUuid(uuid);
        return ResponseEntity.ok(organization);
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<OrganizationResponseDTO> updateOrganization(
            @PathVariable String uuid,
            @RequestBody OrganizationRequestDTO organizationRequestDTO
    ) {
        OrganizationResponseDTO updated = organizationService.updateOrganization(uuid, organizationRequestDTO);
        return ResponseEntity.status(201).body(updated);
    }
}
