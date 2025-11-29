package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import com.qiaben.ciyex.dto.HistoryOfPresentIllnessDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
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
@Component("fhirExternalHistoryOfPresentIllnessStorage")
@RequiredArgsConstructor
@Slf4j
public class FhirExternalHistoryOfPresentIllnessStorage implements ExternalStorage<HistoryOfPresentIllnessDto> {

    private final FhirClientProvider fhirClientProvider;

    private static final String TENANT_TAG_SYSTEM = "http://ciyex.com/tenant";

    @Override
    public String create(HistoryOfPresentIllnessDto dto) {
        String tenantName = tenantName();
        log.info("FHIR HPI create for tenantName={} patientId={} encounterId={}",
                tenantName, dto.getPatientId(), dto.getEncounterId());

        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            Observation observation = mapToFhir(dto, tenantName);
            String externalId = client.create().resource(observation).execute().getId().getIdPart();
            log.info("FHIR create success externalId={} tenantName={}", externalId, tenantName);
            return externalId;
        });
    }

    @Override
    public void update(HistoryOfPresentIllnessDto dto, String externalId) {
        String tenantName = tenantName();
        log.info("FHIR HPI update externalId={} tenantName={}", externalId, tenantName);

        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            Observation observation = mapToFhir(dto, tenantName);
            observation.setId(externalId);
            client.update().resource(observation).execute();
            log.info("FHIR update success externalId={} tenantName={}", externalId, tenantName);
            return null;
        });
    }

    @Override
    public HistoryOfPresentIllnessDto get(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR HPI get externalId={} tenantName={}", externalId, tenantName);

        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            Observation observation = client.read().resource(Observation.class).withId(externalId).execute();
            if (observation == null) {
                log.warn("FHIR resource not found externalId={} tenantName={}", externalId, tenantName);
                return null;
            }

            HistoryOfPresentIllnessDto dto = mapFromFhir(observation);
            log.info("FHIR get success externalId={} tenantName={}", externalId, tenantName);
            return dto;
        });
    }

    @Override
    public void delete(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR HPI delete externalId={} tenantName={}", externalId, tenantName);

        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            client.delete().resourceById("Observation", externalId).execute();
            log.info("FHIR delete success externalId={} tenantName={}", externalId, tenantName);
            return null;
        });
    }

    @Override
    public List<HistoryOfPresentIllnessDto> searchAll() {
        // Not implemented
        return Collections.emptyList();
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return HistoryOfPresentIllnessDto.class.isAssignableFrom(entityType);
    }

    private Observation mapToFhir(HistoryOfPresentIllnessDto dto, String tenantName) {
        Observation observation = new Observation();

        // Set tenant tag
        observation.getMeta().addTag(TENANT_TAG_SYSTEM, tenantName, "Tenant");

        // Set subject (patient)
        observation.setSubject(new Reference("Patient/" + dto.getPatientId()));

        // Set encounter
        if (dto.getEncounterId() != null) {
            observation.setEncounter(new Reference("Encounter/" + dto.getEncounterId()));
        }

        // Set code for HPI
        CodeableConcept code = new CodeableConcept();
        code.setText("History of Present Illness");
        observation.setCode(code);

        // Set value as string
        if (dto.getDescription() != null) {
            observation.setValue(new org.hl7.fhir.r4.model.StringType(dto.getDescription()));
        }

        // Set status
        observation.setStatus(Observation.ObservationStatus.FINAL);

        return observation;
    }

    private HistoryOfPresentIllnessDto mapFromFhir(Observation observation) {
        HistoryOfPresentIllnessDto dto = new HistoryOfPresentIllnessDto();

        // Extract patient ID
        if (observation.getSubject() != null && observation.getSubject().getReference() != null) {
            String patientRef = observation.getSubject().getReference();
            if (patientRef.startsWith("Patient/")) {
                dto.setPatientId(Long.parseLong(patientRef.substring(8)));
            }
        }

        // Extract encounter ID
        if (observation.getEncounter() != null && observation.getEncounter().getReference() != null) {
            String encounterRef = observation.getEncounter().getReference();
            if (encounterRef.startsWith("Encounter/")) {
                dto.setEncounterId(Long.parseLong(encounterRef.substring(10)));
            }
        }

        // Extract description
        if (observation.getValue() instanceof org.hl7.fhir.r4.model.StringType) {
            dto.setDescription(((org.hl7.fhir.r4.model.StringType) observation.getValue()).getValue());
        }

        return dto;
    }

    private String tenantName() {
        return RequestContext.get().getTenantName();
    }

    private <T> T executeWithRetry(Callable<T> operation) {
        int maxRetries = 3;
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                return operation.call();
            } catch (FhirClientConnectionException e) {
                attempt++;
                if (attempt >= maxRetries) {
                    log.error("Failed to execute FHIR operation after {} attempts", maxRetries, e);
                    throw new RuntimeException("FHIR operation failed after retries", e);
                }
                log.warn("FHIR operation failed, retrying... attempt {}/{}", attempt, maxRetries, e);
                try {
                    Thread.sleep(1000 * attempt); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry", ie);
                }
            } catch (Exception e) {
                log.error("Unexpected error during FHIR operation", e);
                throw new RuntimeException("Unexpected error during FHIR operation", e);
            }
        }
        return null;
    }
}
