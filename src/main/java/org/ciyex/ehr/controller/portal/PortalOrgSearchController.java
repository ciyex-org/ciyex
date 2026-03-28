package org.ciyex.ehr.controller.portal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.portal.ApiResponse;
import org.ciyex.ehr.fhir.FhirClientService;
import org.ciyex.ehr.portal.repository.PortalConfigRepository;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Organization;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Public endpoint for searching portal organizations.
 * Used by the patient portal signup page to find the right practice.
 */
@RestController
@RequestMapping("/api/portal/orgs")
@RequiredArgsConstructor
@Slf4j
public class PortalOrgSearchController {

    private final PortalConfigRepository portalConfigRepo;
    private final FhirClientService fhirClientService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> search(
            @RequestParam(required = false, defaultValue = "") String query) {

        String q = query.trim().toLowerCase();

        // Enrich all orgs first (FHIR name + address), then filter by query
        List<Map<String, String>> results = portalConfigRepo.findAll().stream()
                .map(pc -> pc.getOrgAlias())
                .filter(alias -> alias != null && !alias.isBlank() && !"__DEFAULT__".equals(alias))
                .map(this::buildOrgEntry)
                .filter(entry -> q.isEmpty()
                        || entry.getOrDefault("orgName", "").toLowerCase().contains(q)
                        || entry.getOrDefault("orgAlias", "").toLowerCase().contains(q)
                        || entry.getOrDefault("address", "").toLowerCase().contains(q))
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.<List<Map<String, String>>>builder()
                .success(true)
                .message("Organizations found")
                .data(results)
                .build());
    }

    private Map<String, String> buildOrgEntry(String orgAlias) {
        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("orgAlias", orgAlias);
        entry.put("orgName", formatAlias(orgAlias));
        entry.put("address", "");

        try {
            // Search specifically for provider-type Organizations (not payers/insurers)
            Bundle bundle = fhirClientService.searchWithParams(Organization.class, orgAlias,
                    Map.of("type", "prov", "_count", "1"));
            if (bundle.hasEntry() && !bundle.getEntry().isEmpty()) {
                Organization org = (Organization) bundle.getEntry().get(0).getResource();
                if (org.hasName() && !org.getName().isBlank()) {
                    entry.put("orgName", org.getName());
                }
                if (org.hasAddress()) {
                    Address addr = org.getAddressFirstRep();
                    StringBuilder sb = new StringBuilder();
                    if (addr.hasLine()) {
                        sb.append(addr.getLine().get(0).getValue());
                    }
                    if (addr.hasCity()) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(addr.getCity());
                    }
                    if (addr.hasState()) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(addr.getState());
                    }
                    if (addr.hasPostalCode()) {
                        if (sb.length() > 0) sb.append(" ");
                        sb.append(addr.getPostalCode());
                    }
                    entry.put("address", sb.toString());
                }
            }
        } catch (Exception e) {
            log.debug("Could not fetch FHIR Organization for {}: {}", orgAlias, e.getMessage());
        }

        return entry;
    }

    /** Convert "sunrise-family-medicine" → "Sunrise Family Medicine" */
    private String formatAlias(String alias) {
        return Arrays.stream(alias.split("[-_]"))
                .map(w -> w.isEmpty() ? w : Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
