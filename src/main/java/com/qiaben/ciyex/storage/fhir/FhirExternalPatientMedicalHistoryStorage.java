package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import com.qiaben.ciyex.dto.PatientMedicalHistoryDto;
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
@Component("fhirExternalPatientMedicalHistoryStorage")
@RequiredArgsConstructor
@Slf4j
public class FhirExternalPatientMedicalHistoryStorage implements ExternalStorage<PatientMedicalHistoryDto> {

    private final FhirClientProvider fhirClientProvider;

    private static final String TENANT_TAG_SYSTEM = "http://ciyex.com/tenant";

    @Override
    public String create(PatientMedicalHistoryDto dto) {
        String tenantName = tenantName();
        log.info("FHIR PMH create for tenantName={} patientId={} encounterId={}",
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
    public void update(PatientMedicalHistoryDto dto, String externalId) {
        String tenantName = tenantName();
        log.info("FHIR PMH update externalId={} tenantName={}", externalId, tenantName);

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
    public PatientMedicalHistoryDto get(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR PMH get externalId={} tenantName={}", externalId, tenantName);

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

            PatientMedicalHistoryDto dto = mapFromFhir(condition);
            log.info("FHIR get success externalId={} tenantName={}", externalId, tenantName);
            return dto;
        });
    }

    @Override
    public void delete(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR PMH delete externalId={} tenantName={}", externalId, tenantName);

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
    public List<PatientMedicalHistoryDto> searchAll() {
        // Not implemented
        return Collections.emptyList();
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return PatientMedicalHistoryDto.class.isAssignableFrom(entityType);
    }

    private Condition mapToFhir(PatientMedicalHistoryDto dto, String tenantName) {
        Condition condition = new Condition();

        // Set tenant tag
        condition.getMeta().addTag(TENANT_TAG_SYSTEM, tenantName, "Tenant");

        // Set subject (patient)
        condition.setSubject(new Reference("Patient/" + dto.getPatientId()));

        // Set encounter
        if (dto.getEncounterId() != null) {
            condition.setEncounter(new Reference("Encounter/" + dto.getEncounterId()));
        }

        // Set clinical status
        if (dto.getStatus() != null) {
            CodeableConcept clinicalStatus = new CodeableConcept();
            clinicalStatus.setText(dto.getStatus());
            condition.setClinicalStatus(clinicalStatus);
        }

        // Set code (condition/medical condition)
        CodeableConcept code = new CodeableConcept();
        if (dto.getMedicalCondition() != null) {
            code.setText(dto.getMedicalCondition());
        } else if (dto.getConditionName() != null) {
            code.setText(dto.getConditionName());
        } else {
            code.setText("Medical Condition");
        }
        condition.setCode(code);

        // Set onset date
        if (dto.getOnsetDate() != null) {
            condition.setOnset(new DateTimeType(dto.getOnsetDate().toString()));
        }

        // Set abatement (resolved date)
        if (dto.getResolvedDate() != null) {
            condition.setAbatement(new DateTimeType(dto.getResolvedDate().toString()));
        }

        return condition;
    }

    private PatientMedicalHistoryDto mapFromFhir(Condition condition) {
        PatientMedicalHistoryDto dto = new PatientMedicalHistoryDto();

        // Extract patient ID
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

        // Extract status
        if (condition.getClinicalStatus() != null) {
            dto.setStatus(condition.getClinicalStatus().getText());
        }

        // Extract condition name
        if (condition.getCode() != null) {
            dto.setConditionName(condition.getCode().getText());
            dto.setMedicalCondition(condition.getCode().getText());
        }

        // Extract onset date
        if (condition.getOnset() instanceof DateTimeType) {
            DateTimeType onset = (DateTimeType) condition.getOnset();
            if (onset.getValue() != null) {
                dto.setOnsetDate(onset.getValue().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
            }
        }

        // Extract resolved date
        if (condition.getAbatement() instanceof DateTimeType) {
            DateTimeType abatement = (DateTimeType) condition.getAbatement();
            if (abatement.getValue() != null) {
                dto.setResolvedDate(abatement.getValue().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
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
