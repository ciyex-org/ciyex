package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import com.qiaben.ciyex.dto.PlanDto;
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
@Component("fhirExternalPlanStorage")
@RequiredArgsConstructor
@Slf4j
public class FhirExternalPlanStorage implements ExternalStorage<PlanDto> {

    private final FhirClientProvider fhirClientProvider;

    private static final String TENANT_TAG_SYSTEM = "http://ciyex.com/tenant";

    @Override
    public String create(PlanDto dto) {
        String tenantName = tenantName();
        log.info("FHIR create Plan for tenantName={} patientId={} encounterId={}",
                tenantName, dto.getPatientId(), dto.getEncounterId());

        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            CarePlan carePlan = mapToFhir(dto, tenantName);
            String externalId = client.create().resource(carePlan).execute().getId().getIdPart();
            log.info("FHIR create success externalId={} tenantName={}", externalId, tenantName);
            return externalId;
        });
    }

    @Override
    public void update(PlanDto dto, String externalId) {
        String tenantName = tenantName();
        log.info("FHIR update Plan externalId={} tenantName={}", externalId, tenantName);

        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            CarePlan carePlan = mapToFhir(dto, tenantName);
            carePlan.setId(externalId);
            client.update().resource(carePlan).execute();
            log.info("FHIR update success externalId={} tenantName={}", externalId, tenantName);
            return null;
        });
    }

    @Override
    public PlanDto get(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR get Plan externalId={} tenantName={}", externalId, tenantName);

        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            CarePlan carePlan = client.read().resource(CarePlan.class).withId(externalId).execute();
            if (carePlan == null) {
                log.warn("FHIR resource not found externalId={} tenantName={}", externalId, tenantName);
                return null;
            }

            PlanDto dto = mapFromFhir(carePlan);
            log.info("FHIR get success externalId={} tenantName={}", externalId, tenantName);
            return dto;
        });
    }

    @Override
    public void delete(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR delete Plan externalId={} tenantName={}", externalId, tenantName);

        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            client.delete().resourceById("CarePlan", externalId).execute();
            log.info("FHIR delete success externalId={} tenantName={}", externalId, tenantName);
            return null;
        });
    }

    @Override
    public List<PlanDto> searchAll() {
        // Not implemented for now
        return Collections.emptyList();
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return PlanDto.class.isAssignableFrom(entityType);
    }

    private CarePlan mapToFhir(PlanDto dto, String tenantName) {
        CarePlan carePlan = new CarePlan();

        // Set tenant tag
        carePlan.getMeta().addTag(TENANT_TAG_SYSTEM, tenantName, "Tenant");

        // Set subject (patient)
        carePlan.setSubject(new Reference("Patient/" + dto.getPatientId()));

        // Set encounter
        if (dto.getEncounterId() != null) {
            carePlan.setEncounter(new Reference("Encounter/" + dto.getEncounterId()));
        }

        // Set status
        carePlan.setStatus(CarePlan.CarePlanStatus.ACTIVE);

        // Set intent
        carePlan.setIntent(CarePlan.CarePlanIntent.PLAN);

        // Set title and description
        if (dto.getPlan() != null) {
            carePlan.setTitle("Treatment Plan");
            carePlan.setDescription(dto.getPlan());
        }

        // Set notes
        if (dto.getNotes() != null) {
            Annotation note = new Annotation();
            note.setText(dto.getNotes());
            carePlan.addNote(note);
        }

        return carePlan;
    }

    private PlanDto mapFromFhir(CarePlan carePlan) {
        PlanDto dto = new PlanDto();

        // Extract patient ID
        if (carePlan.getSubject() != null && carePlan.getSubject().getReference() != null) {
            String patientRef = carePlan.getSubject().getReference();
            if (patientRef.startsWith("Patient/")) {
                dto.setPatientId(Long.parseLong(patientRef.substring(8)));
            }
        }

        // Extract encounter ID
        if (carePlan.getEncounter() != null && carePlan.getEncounter().getReference() != null) {
            String encounterRef = carePlan.getEncounter().getReference();
            if (encounterRef.startsWith("Encounter/")) {
                dto.setEncounterId(Long.parseLong(encounterRef.substring(10)));
            }
        }

        // Extract plan text
        if (carePlan.getDescription() != null) {
            dto.setPlan(carePlan.getDescription());
        }

        // Extract notes
        if (!carePlan.getNote().isEmpty()) {
            dto.setNotes(carePlan.getNote().get(0).getText());
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
