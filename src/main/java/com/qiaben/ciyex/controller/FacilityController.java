package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.FacilityRequestDTO;
import com.qiaben.ciyex.dto.FacilityResponseDTO;
import com.qiaben.ciyex.dto.StandardApiResponse;
import com.qiaben.ciyex.service.FacilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/facility")
@RequiredArgsConstructor
public class FacilityController {

    private final FacilityService facilityService;

    @GetMapping
    public ResponseEntity<StandardApiResponse<FacilityResponseDTO>> getFacility(
            @RequestParam(required = false) String name,
            @RequestParam(required = false, name = "facility_npi") String facilityNpi,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String fax,
            @RequestParam(required = false) String street,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            @RequestParam(required = false, name = "postal_code") String postalCode,
            @RequestParam(required = false, name = "country_code") String countryCode,
            @RequestParam(required = false, name = "federal_ein") String federalEin,
            @RequestParam(required = false) String website,
            @RequestParam(required = false) String email,
            @RequestParam(required = false, name = "domain_identifier") String domainIdentifier,
            @RequestParam(required = false, name = "facility_taxonomy") String facilityTaxonomy,
            @RequestParam(required = false, name = "facility_code") String facilityCode,
            @RequestParam(required = false, name = "billing_location") String billingLocation,
            @RequestParam(required = false, name = "accepts_assignment") String acceptsAssignment,
            @RequestParam(required = false) String oid,
            @RequestParam(required = false, name = "service_location") String serviceLocation
    ) {
        FacilityRequestDTO filter = new FacilityRequestDTO();
        filter.setName(name);
        filter.setFacilityNpi(facilityNpi);
        filter.setPhone(phone);
        filter.setFax(fax);
        filter.setStreet(street);
        filter.setCity(city);
        filter.setState(state);
        filter.setPostalCode(postalCode);
        filter.setCountryCode(countryCode);
        filter.setFederalEin(federalEin);
        filter.setWebsite(website);
        filter.setEmail(email);
        filter.setDomainIdentifier(domainIdentifier);
        filter.setFacilityTaxonomy(facilityTaxonomy);
        filter.setFacilityCode(facilityCode);
        filter.setBillingLocation(billingLocation);
        filter.setAcceptsAssignment(acceptsAssignment);
        filter.setOid(oid);
        filter.setServiceLocation(serviceLocation);

        List<FacilityResponseDTO> data = facilityService.getFacilities(filter);

        StandardApiResponse<FacilityResponseDTO> response = StandardApiResponse.<FacilityResponseDTO>builder()
                .validationErrors(List.of())
                .internalErrors(List.of())
                .data(data)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<StandardApiResponse<FacilityResponseDTO>> createFacility(
            @RequestBody FacilityRequestDTO request
    ) {
        FacilityResponseDTO facility = facilityService.createFacility(request);
        StandardApiResponse<FacilityResponseDTO> response = StandardApiResponse.<FacilityResponseDTO>builder()
                .validationErrors(List.of())
                .internalErrors(List.of())
                .data(List.of(facility))
                .build();
        return ResponseEntity.ok(response);
    }
}
