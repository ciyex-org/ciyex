package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import com.qiaben.ciyex.dto.ChiefComplaintDto;
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
@Component("fhirExternalChiefComplaintStorage")
@RequiredArgsConstructor
@Slf4j
public class FhirExternalChiefComplaintStorage implements ExternalStorage<ChiefComplaintDto> {

    private final FhirClientProvider fhirClientProvider;

    private static final String TENANT_TAG_SYSTEM = "http://ciyex.com/tenant";

    @Override
    public String create(ChiefComplaintDto dto) {
        String tenantName = tenantName();
        log.info("FHIR create ChiefComplaint for tenantName={} patientId={} encounterId={}",
                tenantName, dto.getPatientId(), dto.getEncounterId());

        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            Condition condition = mapToFhir(dto, tenantName);
            String externalId = client.create().resource(condition).execute().getId().getIdPart();
            log.info("FHIR create success externalId={} tenantName={}", externalId, tenantName);
            return externalId;
        });
    }

    @Override
    public void update(ChiefComplaintDto dto, String externalId) {
        String tenantName = tenantName();
        log.info("FHIR update ChiefComplaint externalId={} tenantName={}", externalId, tenantName);

        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            Condition condition = mapToFhir(dto, tenantName);
            condition.setId(externalId);
            client.update().resource(condition).execute();
            log.info("FHIR update success externalId={} tenantName={}", externalId, tenantName);
            return null;
        });
    }

    @Override
    public void delete(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR delete ChiefComplaint externalId={} tenantName={}", externalId, tenantName);

        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            client.delete().resourceById("Condition", externalId).execute();
            log.info("FHIR delete success externalId={} tenantName={}", externalId, tenantName);
            return null;
        });
    }

    @Override
    public ChiefComplaintDto get(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR get ChiefComplaint externalId={} tenantName={}", externalId, tenantName);

        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            Condition condition = client.read().resource(Condition.class).withId(externalId).execute();
            if (condition == null) {
                log.warn("FHIR resource not found externalId={} tenantName={}", externalId, tenantName);
                return null;
            }

            ChiefComplaintDto dto = mapFromFhir(condition);
            log.info("FHIR get success externalId={} tenantName={}", externalId, tenantName);
            return dto;
        });
    }

    private Condition mapToFhir(ChiefComplaintDto dto, String tenantName) {
        Condition condition = new Condition();

        // Set tenant tag
        condition.getMeta().addTag(TENANT_TAG_SYSTEM, tenantName, "Tenant");

        // Set patient reference
        condition.setSubject(new Reference("Patient/" + dto.getPatientId()));

        // Set encounter reference if available
        if (dto.getEncounterId() != null) {
            condition.setEncounter(new Reference("Encounter/" + dto.getEncounterId()));
        }

        // Set code (complaint)
        if (dto.getComplaint() != null) {
            CodeableConcept code = new CodeableConcept();
            code.setText(dto.getComplaint());
            condition.setCode(code);
        }

        // Set clinical status
        if (dto.getStatus() != null) {
            condition.setClinicalStatus(new CodeableConcept().setText(dto.getStatus()));
        }

        // Set severity
        if (dto.getSeverity() != null) {
            condition.setSeverity(new CodeableConcept().setText(dto.getSeverity()));
        }

        // Set notes in extension or annotation
        if (dto.getDetails() != null) {
            Annotation note = new Annotation();
            note.setText(dto.getDetails());
            condition.addNote(note);
        }

        return condition;
    }

    private ChiefComplaintDto mapFromFhir(Condition condition) {
        ChiefComplaintDto dto = new ChiefComplaintDto();

        // Extract patient ID from subject reference
        if (condition.getSubject() != null && condition.getSubject().getReference() != null) {
            String patientRef = condition.getSubject().getReference();
            if (patientRef.startsWith("Patient/")) {
                dto.setPatientId(Long.parseLong(patientRef.substring(8)));
            }
        }

        // Extract encounter ID
        if (condition.getEncounter() != null && condition.getEncounter().getReference() != null) {
            String encounterRef = condition.getEncounter().getReference();
            if (encounterRef.startsWith("Encounter/")) {
                dto.setEncounterId(Long.parseLong(encounterRef.substring(10)));
            }
        }

        // Extract complaint
        if (condition.getCode() != null) {
            dto.setComplaint(condition.getCode().getText());
        }

        // Extract status
        if (condition.getClinicalStatus() != null) {
            dto.setStatus(condition.getClinicalStatus().getText());
        }

        // Extract severity
        if (condition.getSeverity() != null) {
            dto.setSeverity(condition.getSeverity().getText());
        }

        // Extract details
        if (!condition.getNote().isEmpty()) {
            dto.setDetails(condition.getNote().get(0).getText());
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

    @Override
    public List<ChiefComplaintDto> searchAll() {
        // Not implemented for now
        return Collections.emptyList();
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return ChiefComplaintDto.class.isAssignableFrom(entityType);
    }
}
