package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import com.qiaben.ciyex.dto.AssignedProviderDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.StorageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@StorageType("fhir")
@Component("fhirExternalAssignedProviderStorage")
@RequiredArgsConstructor
@Slf4j
public class FhirExternalAssignedProviderStorage implements ExternalStorage<AssignedProviderDto> {

    private final FhirClientProvider fhirClientProvider;

    private static final String TENANT_TAG_SYSTEM = "http://ciyex.com/tenant";

    @Override
    public String create(AssignedProviderDto dto) {
        String tenantName = tenantName();
        log.info("FHIR create AssignedProvider (CareTeam) for tenantName={} patientId={} encounterId={} providerId={}",
                tenantName, dto.getPatientId(), dto.getEncounterId(), dto.getProviderId());

        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            CareTeam careTeam = mapToFhir(dto, tenantName);
            String externalId = client.create().resource(careTeam).execute().getId().getIdPart();
            log.info("FHIR create success externalId={} tenantName={}", externalId, tenantName);
            return externalId;
        });
    }

    @Override
    public void update(AssignedProviderDto dto, String externalId) {
        String tenantName = tenantName();
        log.info("FHIR update AssignedProvider (CareTeam) externalId={} tenantName={}", externalId, tenantName);

        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping update.");
                return null;
            }

            CareTeam careTeam = mapToFhir(dto, tenantName);
            careTeam.setId(externalId);
            client.update().resource(careTeam).execute();
            log.info("FHIR update success externalId={} tenantName={}", externalId, tenantName);
            return null;
        });
    }

    @Override
    public AssignedProviderDto get(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR get AssignedProvider (CareTeam) externalId={} tenantName={}", externalId, tenantName);

        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Cannot retrieve resource.");
                return null;
            }

            CareTeam careTeam = client.read().resource(CareTeam.class).withId(externalId).execute();

            // enforce tenant isolation via meta.tag
            if (!resourceBelongsToOrg(careTeam, tenantName)) {
                throw new SecurityException("CareTeam does not belong to tenantName=" + tenantName);
            }

            AssignedProviderDto dto = mapFromFhir(careTeam);
            dto.setExternalId(externalId);
            dto.setFhirId(externalId);  // fhirId is same as externalId
            log.debug("FHIR get mapped dto externalId={} tenantName={}", externalId, tenantName);
            return dto;
        });
    }

    @Override
    public void delete(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR delete AssignedProvider (CareTeam) externalId={} tenantName={}", externalId, tenantName);

        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping delete.");
                return null;
            }

            client.delete().resourceById("CareTeam", externalId).execute();
            log.info("FHIR delete success externalId={} tenantName={}", externalId, tenantName);
            return null;
        });
    }

    @Override
    public List<AssignedProviderDto> searchAll() {
        String tenantName = tenantName();
        log.info("FHIR searchAll AssignedProviders (CareTeam) tenantName={}", tenantName);

        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();

            Bundle bundle = client.search()
                    .forResource(CareTeam.class)
                    .where(new TokenClientParam("_tag").exactly()
                            .systemAndCode(TENANT_TAG_SYSTEM, tenantName != null ? tenantName : ""))
                    .returnBundle(Bundle.class)
                    .execute();

            final List<Bundle.BundleEntryComponent> entries = new ArrayList<>(bundle.getEntry());
            while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
                bundle = client.loadPage().next(bundle).execute();
                entries.addAll(bundle.getEntry());
            }

            List<AssignedProviderDto> out = entries.stream()
                    .map(e -> (CareTeam) e.getResource())
                    .map(careTeam -> {
                        AssignedProviderDto dto = mapFromFhir(careTeam);
                        String fhirId = careTeam.getIdElement().getIdPart();
                        dto.setExternalId(fhirId);
                        dto.setFhirId(fhirId);  // fhirId is same as externalId
                        return dto;
                    })
                    .collect(Collectors.toList());

            log.info("FHIR searchAll found {} AssignedProviders for tenantName={}", out.size(), tenantName);
            return out;
        });
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return AssignedProviderDto.class.isAssignableFrom(entityType);
    }

    private CareTeam mapToFhir(AssignedProviderDto dto, String tenantName) {
        CareTeam careTeam = new CareTeam();
        careTeam.setStatus(CareTeam.CareTeamStatus.ACTIVE);

        // Add tenant tag
        careTeam.getMeta()
                .addTag()
                .setSystem(TENANT_TAG_SYSTEM)
                .setCode(tenantName != null ? tenantName : "");

        // Store metadata in name field
        String metadata = safe(dto.getPatientId()) + "|" +
                safe(dto.getEncounterId()) + "|" +
                safe(dto.getProviderId()) + "|" +
                safe(dto.getRole()) + "|" +
                safe(dto.getStartDate()) + "|" +
                safe(dto.getEndDate()) + "|" +
                safe(dto.getStatus()) + "|" +
                safe(dto.getNotes());
        careTeam.setName(metadata);

        // Add participant if providerId exists
        if (dto.getProviderId() != null) {
            CareTeam.CareTeamParticipantComponent participant = new CareTeam.CareTeamParticipantComponent();

            // Set role
            if (dto.getRole() != null) {
                CodeableConcept roleCode = new CodeableConcept();
                roleCode.setText(dto.getRole());
                participant.addRole(roleCode);
            }

            // Set member reference (Practitioner)
            Reference practitionerRef = new Reference("Practitioner/" + dto.getProviderId());
            participant.setMember(practitionerRef);

            // Set period
            if (dto.getStartDate() != null || dto.getEndDate() != null) {
                Period period = new Period();
                if (dto.getStartDate() != null) {
                    try {
                        period.setStartElement(new DateTimeType(dto.getStartDate()));
                    } catch (Exception e) {
                        log.warn("Failed to parse startDate: {}", dto.getStartDate());
                    }
                }
                if (dto.getEndDate() != null) {
                    try {
                        period.setEndElement(new DateTimeType(dto.getEndDate()));
                    } catch (Exception e) {
                        log.warn("Failed to parse endDate: {}", dto.getEndDate());
                    }
                }
                participant.setPeriod(period);
            }

            careTeam.addParticipant(participant);
        }

        return careTeam;
    }

    private AssignedProviderDto mapFromFhir(CareTeam careTeam) {
        AssignedProviderDto dto = new AssignedProviderDto();

        // externalId
        dto.setExternalId(careTeam.getIdElement().getIdPart());

        // Parse metadata from name field
        String metadata = careTeam.getName();
        String[] parts = metadata != null ? metadata.split("\\|", -1) : new String[0];

        dto.setPatientId(parseLong(partOrNull(parts, 0)));
        dto.setEncounterId(parseLong(partOrNull(parts, 1)));
        dto.setProviderId(parseLong(partOrNull(parts, 2)));
        dto.setRole(partOrNull(parts, 3));
        dto.setStartDate(partOrNull(parts, 4));
        dto.setEndDate(partOrNull(parts, 5));
        dto.setStatus(partOrNull(parts, 6));
        dto.setNotes(partOrNull(parts, 7));

        return dto;
    }

    private <T> T executeWithRetry(FhirOperation<T> operation) {
        String tenantName = tenantName();
        try {
            return operation.execute();
        } catch (FhirClientConnectionException e) {
            log.error("FHIR connection error tenantName={} status={} msg={}", tenantName, e.getStatusCode(), e.getMessage());
            if (e.getStatusCode() == 401) {
                log.warn("401 unauthorized; retrying once with fresh client tenantName={}", tenantName);
                try {
                    return operation.execute();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
            throw e;
        } catch (Exception e) {
            log.error("Unexpected FHIR error tenantName={} msg={}", tenantName, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    private interface FhirOperation<T> {
        T execute();
    }

    private String tenantName() {
        return RequestContext.get() != null ? RequestContext.get().getTenantName() : null;
    }

    private static String safe(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    private static String partOrNull(String[] arr, int idx) {
        if (arr == null || idx < 0 || idx >= arr.length) return null;
        String v = arr[idx];
        return (v == null || v.isEmpty()) ? null : v;
    }

    private boolean resourceBelongsToOrg(CareTeam careTeam, String tenantName) {
        // Check meta.tag
        boolean tagMatch = careTeam.getMeta() != null && careTeam.getMeta().getTag().stream()
                .anyMatch(t -> TENANT_TAG_SYSTEM.equals(t.getSystem()) && tenantName.equals(t.getCode()));
        return tagMatch;
    }

    private static Long parseLong(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
