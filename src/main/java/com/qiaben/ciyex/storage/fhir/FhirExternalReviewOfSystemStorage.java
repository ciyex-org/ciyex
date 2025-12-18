package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiaben.ciyex.dto.ReviewOfSystemDto;
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
@Component("fhirExternalReviewOfSystemStorage")
@RequiredArgsConstructor
@Slf4j
public class FhirExternalReviewOfSystemStorage implements ExternalStorage<ReviewOfSystemDto> {

    private final FhirClientProvider fhirClientProvider;

    private static final String TENANT_TAG_SYSTEM = "http://ciyex.com/tenant";

    @Override
    public String create(ReviewOfSystemDto dto) {
        String tenantName = tenantName();
        log.info("FHIR create ReviewOfSystem for tenantName={} patientId={} encounterId={}",
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
    public void update(ReviewOfSystemDto dto, String externalId) {
        String tenantName = tenantName();
        log.info("FHIR update ReviewOfSystem externalId={} tenantName={}", externalId, tenantName);

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
    public ReviewOfSystemDto get(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR get ReviewOfSystem externalId={} tenantName={}", externalId, tenantName);

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

            ReviewOfSystemDto dto = mapFromFhir(observation);
            log.info("FHIR get success externalId={} tenantName={}", externalId, tenantName);
            return dto;
        });
    }

    @Override
    public void delete(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR delete ReviewOfSystem externalId={} tenantName={}", externalId, tenantName);

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
    public List<ReviewOfSystemDto> searchAll() {
        // Not implemented for ReviewOfSystem
        return Collections.emptyList();
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return ReviewOfSystemDto.class.isAssignableFrom(entityType);
    }

    private Observation mapToFhir(ReviewOfSystemDto dto, String tenantName) {
        Observation observation = new Observation();
        observation.setStatus(Observation.ObservationStatus.FINAL);

        // Set category to exam for ROS
        observation.addCategory(new CodeableConcept().addCoding(
            new Coding().setSystem("http://terminology.hl7.org/CodeSystem/observation-category").setCode("exam")
        ));

        // Set code for Review of Systems
        observation.setCode(new CodeableConcept().setText("Review of Systems"));

        // Set subject (patient)
        if (dto.getPatientId() != null) {
            observation.setSubject(new Reference("Patient/" + dto.getPatientId()));
        }

        // Set encounter
        if (dto.getEncounterId() != null) {
            observation.setEncounter(new Reference("Encounter/" + dto.getEncounterId()));
        }

        // Serialize entire DTO as JSON and store as note
        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonData = mapper.writeValueAsString(dto);
            observation.addNote(new Annotation().setText(jsonData));
        } catch (Exception e) {
            log.error("Failed to serialize ROS DTO to JSON", e);
        }

        // Add tenant tag
        observation.getMeta().addTag(TENANT_TAG_SYSTEM, tenantName, "Tenant");

        return observation;
    }

    private ReviewOfSystemDto mapFromFhir(Observation observation) {
        // Deserialize from JSON stored in note
        if (!observation.getNote().isEmpty()) {
            try {
                String jsonData = observation.getNote().get(0).getText();
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(jsonData, ReviewOfSystemDto.class);
            } catch (Exception e) {
                log.error("Failed to deserialize ROS DTO from JSON", e);
            }
        }

        // Fallback: create empty DTO with basic info
        ReviewOfSystemDto dto = new ReviewOfSystemDto();
        if (observation.getSubject() != null && observation.getSubject().getReference() != null) {
            String patientRef = observation.getSubject().getReference();
            if (patientRef.startsWith("Patient/")) {
                dto.setPatientId(Long.parseLong(patientRef.substring(8)));
            }
        }
        if (observation.getEncounter() != null && observation.getEncounter().getReference() != null) {
            String encounterRef = observation.getEncounter().getReference();
            if (encounterRef.startsWith("Encounter/")) {
                dto.setEncounterId(Long.parseLong(encounterRef.substring(10)));
            }
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
