package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import com.qiaben.ciyex.dto.SocialHistoryDto;
import com.qiaben.ciyex.dto.SocialHistoryEntryDto;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.StorageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.Callable;

@StorageType("fhir")
@Component("fhirExternalSocialHistoryStorage")
@RequiredArgsConstructor
@Slf4j
public class FhirExternalSocialHistoryStorage implements ExternalStorage<SocialHistoryDto> {

    private final FhirClientProvider fhirClientProvider;

    private static final String TENANT_TAG_SYSTEM = "http://ciyex.com/tenant";

    @Override
    public String create(SocialHistoryDto dto) {
        String tenantName = tenantName();
        log.info("FHIR create SocialHistory for tenantName={} patientId={} encounterId={}",
                tenantName, dto.getPatientId(), dto.getEncounterId());

        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            Bundle bundle = mapToFhir(dto, tenantName);
            Bundle response = client.transaction().withBundle(bundle).execute();
            String externalId = response.getId() != null ? response.getId() : "SH-" + System.currentTimeMillis();
            log.info("FHIR create success externalId={} tenantName={}", externalId, tenantName);
            return externalId;
        });
    }

    @Override
    public void update(SocialHistoryDto dto, String externalId) {
        String tenantName = tenantName();
        log.info("FHIR update SocialHistory externalId={} tenantName={}", externalId, tenantName);

        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            Bundle bundle = mapToFhir(dto, tenantName);
            client.transaction().withBundle(bundle).execute();
            log.info("FHIR update success externalId={} tenantName={}", externalId, tenantName);
            return null;
        });
    }

    @Override
    public SocialHistoryDto get(String externalId) {
        // Not implemented for now
        return null;
    }

    @Override
    public void delete(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR delete SocialHistory externalId={} tenantName={}", externalId, tenantName);

        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            // Assume externalId is the bundle ID or something - for simplicity, skip actual delete
            log.info("FHIR delete skipped for externalId={} tenantName={}", externalId, tenantName);
            return null;
        });
    }

    public List<SocialHistoryDto> searchAll(Long patientId) {
        // Not implemented
        return Collections.emptyList();
    }

    public List<SocialHistoryDto> searchAll(Long patientId, Long encounterId) {
        // Not implemented
        return Collections.emptyList();
    }

    public List<SocialHistoryDto> searchAll() {
        return Collections.emptyList();
    }

    public boolean supports(Class<?> clazz) {
        return SocialHistoryDto.class.isAssignableFrom(clazz);
    }

    private Bundle mapToFhir(SocialHistoryDto dto, String tenantName) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.TRANSACTION);

        // Create Observations for each entry
        for (SocialHistoryEntryDto entry : dto.getEntries()) {
            Observation obs = new Observation();
            obs.setStatus(Observation.ObservationStatus.FINAL);
            obs.getCategoryFirstRep().addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
                .setCode("social-history")
                .setDisplay("Social History");

            obs.getCode().addCoding()
                .setSystem("http://ciyex.com/social-history")
                .setCode(entry.getCategory())
                .setDisplay(entry.getCategory());

            if (entry.getValue() != null) {
                obs.setValue(new StringType(entry.getValue()));
            }

            if (entry.getDetails() != null) {
                obs.setNote(Collections.singletonList(new Annotation().setText(entry.getDetails())));
            }

            // Set subject and encounter
            obs.setSubject(new Reference("Patient/" + dto.getPatientId()));
            obs.setEncounter(new Reference("Encounter/" + dto.getEncounterId()));

            // Add tenant tag
            obs.getMeta().addTag(TENANT_TAG_SYSTEM, tenantName, null);

            Bundle.BundleEntryComponent entryComp = new Bundle.BundleEntryComponent();
            entryComp.setResource(obs);
            entryComp.getRequest().setMethod(Bundle.HTTPVerb.POST).setUrl("Observation");
            bundle.addEntry(entryComp);
        }

        return bundle;
    }

    private String tenantName() {
        // Implement tenant name retrieval
        return "default"; // Placeholder
    }

    private <T> T executeWithRetry(Callable<T> operation) {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return operation.call();
            } catch (FhirClientConnectionException e) {
                if (attempt == maxRetries) {
                    throw new RuntimeException(e);
                }
                log.warn("FHIR operation failed, retrying... attempt {}/{}", attempt, maxRetries, e);
                try {
                    Thread.sleep(1000 * attempt); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ie);
                }
            } catch (Exception e) {
                log.error("Unexpected error during FHIR operation", e);
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}
