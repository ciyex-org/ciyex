package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import com.qiaben.ciyex.dto.CoverageDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@StorageType("fhir")
@Component("fhirExternalCoverageStorage")
@Slf4j
public class FhirExternalCoverageStorage implements ExternalStorage<CoverageDto> {

    private final FhirClientProvider fhirClientProvider;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    // Keep these constants consistent across your project
    private static final String ORG_TAG_SYSTEM = "http://ciyex.com/tenant"; // used as _tag system
    private static final String ORG_EXT_URL   = "http://ciyex.com/fhir/StructureDefinition/orgId";
    private static final String POLICY_ID_SYSTEM = "http://ciyex.com/coverage/policy-number";

    @Autowired
    public FhirExternalCoverageStorage(FhirClientProvider fhirClientProvider) {
        this.fhirClientProvider = fhirClientProvider;
        log.info("Initializing FhirExternalCoverageStorage with FhirClientProvider");
    }

    @Override
    public String create(CoverageDto entityDto) {
        Long orgId = currentOrgId();
        log.info("Entering create for orgId: {}, coveragePolicyNumber: {}", orgId, entityDto.getPolicyNumber());
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            Coverage fhirCoverage = mapToFhirCoverage(entityDto, orgId);
            String externalId = client.create().resource(fhirCoverage).execute().getId().getIdPart();
            log.info("Created Coverage with externalId: {} for orgId: {}", externalId, orgId);
            return externalId;
        });
    }

    @Override
    public void update(CoverageDto entityDto, String externalId) {
        Long orgId = currentOrgId();
        log.info("Entering update for orgId: {}, externalId: {}, policyNumber: {}", orgId, externalId, entityDto.getPolicyNumber());
        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            Coverage fhirCoverage = mapToFhirCoverage(entityDto, orgId);
            fhirCoverage.setId(externalId);
            client.update().resource(fhirCoverage).execute();
            log.info("Updated Coverage with externalId: {} for orgId: {}", externalId, orgId);
            return null;
        });
    }

    @Override
    public CoverageDto get(String externalId) {
        Long orgId = currentOrgId();
        log.info("Entering get for orgId: {}, externalId: {}", orgId, externalId);
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            Coverage fhirCoverage = client.read().resource(Coverage.class).withId(externalId).execute();

            // enforce tenant isolation via meta.tag OR extension
            if (!resourceBelongsToOrg(fhirCoverage, orgId)) {
                throw new SecurityException("Coverage does not belong to orgId=" + orgId);
            }

            CoverageDto coverageDto = mapFromFhirCoverage(fhirCoverage);
            // carry orgId from RequestContext; you can also set it from tag if you prefer
            coverageDto.setOrgId(orgId);
            log.info("Retrieved CoverageDto with externalId: {} for orgId: {}", externalId, orgId);
            return coverageDto;
        });
    }

    @Override
    public void delete(String externalId) {
        Long orgId = currentOrgId();
        log.info("Entering delete for orgId: {}, externalId: {}", orgId, externalId);
        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            // (Optional) read first and validate org before delete
            try {
                Coverage existing = client.read().resource(Coverage.class).withId(externalId).execute();
                if (!resourceBelongsToOrg(existing, orgId)) {
                    throw new SecurityException("Coverage does not belong to orgId=" + orgId);
                }
            } catch (Exception readErr) {
                log.warn("Skip pre-delete read validation: {}", readErr.getMessage());
            }
            client.delete().resourceById("Coverage", externalId).execute();
            log.info("Deleted Coverage with externalId: {} for orgId: {}", externalId, orgId);
            return null;
        });
    }

    @Override
    public List<CoverageDto> searchAll() {
        Long orgId = currentOrgId();
        log.info("Entering searchAll for orgId: {}", orgId);
        Bundle bundle = fhirClientProvider.getForCurrentOrg().search()
                .forResource(Coverage.class)
                // this depends on the meta.tag, which we now set in mapToFhirCoverage(...)
                .where(new TokenClientParam("_tag").exactly().systemAndCode(ORG_TAG_SYSTEM, String.valueOf(orgId)))
                .returnBundle(Bundle.class)
                .execute();

        log.debug("Received Bundle with {} entries for orgId: {}", bundle.getEntry().size(), orgId);
        return bundle.getEntry().stream()
                .map(entry -> (Coverage) entry.getResource())
                .map(cov -> {
                    CoverageDto dto = mapFromFhirCoverage(cov);
                    dto.setExternalId(cov.getIdElement().getIdPart());
                    dto.setOrgId(orgId); // from RequestContext
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return CoverageDto.class.isAssignableFrom(entityType);
    }

    // -------- Helpers --------

    private Long currentOrgId() {
        return Optional.ofNullable(RequestContext.get())
                .map(RequestContext::getOrgId)
                .orElseThrow(() -> new SecurityException("No orgId in RequestContext"));
    }

    private boolean resourceBelongsToOrg(Coverage cov, Long orgId) {
        // Prefer meta.tag check
        boolean tagMatch = cov.getMeta() != null && cov.getMeta().getTag().stream()
                .anyMatch(t -> ORG_TAG_SYSTEM.equals(t.getSystem()) && String.valueOf(orgId).equals(t.getCode()));

        if (tagMatch) return true;

        // Fallback: extension check
        boolean extMatch = cov.getExtension().stream()
                .anyMatch(ext -> ORG_EXT_URL.equals(ext.getUrl())
                        && ext.getValue() instanceof StringType
                        && String.valueOf(orgId).equals(((StringType) ext.getValue()).getValue()));

        return extMatch;
    }

    private <T> T executeWithRetry(FhirOperation<T> operation) {
        try {
            return operation.execute();
        } catch (FhirClientConnectionException e) {
            // Depending on your stack, status code may be on a different exception; adapt if needed
            log.warn("FHIR connection error: {}. Retrying once...", e.getMessage());
            return operation.execute(); // retry once (e.g., refreshed client inside provider)
        }
    }

    @FunctionalInterface
    private interface FhirOperation<T> {
        T execute();
    }

    private Coverage mapToFhirCoverage(CoverageDto dto, Long orgId) {
        Coverage f = new Coverage();

        // --- Meta tag for tenant filtering ---
        f.getMeta().addTag().setSystem(ORG_TAG_SYSTEM).setCode(String.valueOf(orgId));

        // --- Identifier (policy number) ---
        if (dto.getPolicyNumber() != null) {
            f.addIdentifier(new Identifier()
                    .setSystem(POLICY_ID_SYSTEM)
                    .setValue(dto.getPolicyNumber()));
        }

        // --- Period ---
        Period period = new Period();
        Date start = parseDateSafe(dto.getCoverageStartDate());
        Date end   = parseDateSafe(dto.getCoverageEndDate());
        if (start != null) period.setStart(start);
        if (end != null)   period.setEnd(end);
        if (start != null || end != null) {
            f.setPeriod(period);
        }

        // --- (Optional) Beneficiary / Payor references could be set here if you have them ---
        // e.g., f.setBeneficiary(new Reference("Patient/" + dto.getPatientExternalId()));
        // e.g., f.addPayor(new Reference("Organization/" + payerId));

        // --- OrgId extension (optional but useful) ---
        f.addExtension(new Extension()
                .setUrl(ORG_EXT_URL)
                .setValue(new StringType(String.valueOf(orgId))));

        return f;
    }

    private CoverageDto mapFromFhirCoverage(Coverage f) {
        CoverageDto dto = new CoverageDto();

        // externalId
        dto.setExternalId(f.getIdElement().getIdPart());

        // identifier (policy number)
        if (f.hasIdentifier()) {
            Identifier id = f.getIdentifierFirstRep();
            dto.setPolicyNumber(id != null ? id.getValue() : null);
        }

        // period
        if (f.hasPeriod()) {
            if (f.getPeriod().hasStart()) {
                dto.setCoverageStartDate(f.getPeriod().getStartElement().getValueAsString());
            }
            if (f.getPeriod().hasEnd()) {
                dto.setCoverageEndDate(f.getPeriod().getEndElement().getValueAsString());
            }
        }

        // (Optional) read orgId from meta tag or extension if you want
        // Long orgId = extractOrgId(f);
        // dto.setOrgId(orgId);

        return dto;
    }

    @SuppressWarnings("unused")
    private Long extractOrgId(Coverage f) {
        // Prefer meta tag
        if (f.getMeta() != null) {
            for (Coding t : f.getMeta().getTag()) {
                if (ORG_TAG_SYSTEM.equals(t.getSystem())) {
                    try {
                        return Long.parseLong(t.getCode());
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        // Fallback: extension
        for (Extension ext : f.getExtension()) {
            if (ORG_EXT_URL.equals(ext.getUrl()) && ext.getValue() instanceof StringType) {
                try {
                    return Long.parseLong(((StringType) ext.getValue()).getValue());
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    private Date parseDateSafe(String ymd) {
        if (ymd == null || ymd.isEmpty()) return null;
        try {
            return DATE_FORMAT.parse(ymd);
        } catch (Exception e) {
            log.warn("Invalid date '{}', expected yyyy-MM-dd", ymd);
            return null;
        }
    }
}
