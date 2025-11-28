package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import com.qiaben.ciyex.dto.EntryDto;
import com.qiaben.ciyex.dto.FamilyHistoryDto;

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
import java.util.stream.Collectors;

@StorageType("fhir")
@Component("fhirExternalFamilyHistoryStorage")
@RequiredArgsConstructor
@Slf4j
public class FhirExternalFamilyHistoryStorage implements ExternalStorage<FamilyHistoryDto> {

    private final FhirClientProvider fhirClientProvider;

    private static final String TENANT_TAG_SYSTEM = "http://ciyex.com/tenant";

    @Override
    public String create(FamilyHistoryDto dto) {
        String tenantName = tenantName();
        log.info("FHIR FH create for tenantName={} patientId={} encounterId={}",
                tenantName, dto.getPatientId(), dto.getEncounterId());

        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            List<String> ids = new ArrayList<>();
            if (dto.getEntries() != null) {
                for (var entry : dto.getEntries()) {
                    FamilyMemberHistory fmh = mapToFhir(dto, entry, tenantName);
                    String id = client.create().resource(fmh).execute().getId().getIdPart();
                    ids.add(id);
                }
            }
            // Return the first ID or null if no entries
            return ids.isEmpty() ? null : ids.get(0);
        });
    }

    @Override
    public void update(FamilyHistoryDto dto, String externalId) {
        String tenantName = tenantName();
        log.info("FHIR FH update externalId={} tenantName={}", externalId, tenantName);

        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            // For simplicity, assume externalId is the first entry's ID, but updating all might be complex
            // TODO: Implement proper update logic, perhaps delete old and create new
            log.warn("Update not fully implemented for FamilyHistory FHIR");
            return null;
        });
    }

    @Override
    public FamilyHistoryDto get(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR FH get externalId={} tenantName={}", externalId, tenantName);

        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            FamilyMemberHistory fmh = client.read().resource(FamilyMemberHistory.class).withId(externalId).execute();
            if (fmh == null) {
                log.warn("FHIR resource not found externalId={} tenantName={}", externalId, tenantName);
                return null;
            }

            // Map back to DTO, but since multiple entries, this is incomplete
            FamilyHistoryDto dto = mapFromFhir(fmh);
            log.info("FHIR get success externalId={} tenantName={}", externalId, tenantName);
            return dto;
        });
    }

    @Override
    public void delete(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR FH delete externalId={} tenantName={}", externalId, tenantName);

        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            client.delete().resourceById("FamilyMemberHistory", externalId).execute();
            log.info("FHIR delete success externalId={} tenantName={}", externalId, tenantName);
            return null;
        });
    }

    @Override
    public List<FamilyHistoryDto> searchAll() {
        // Not implemented
        return Collections.emptyList();
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return FamilyHistoryDto.class.isAssignableFrom(entityType);
    }

    private FamilyMemberHistory mapToFhir(FamilyHistoryDto dto, EntryDto entry, String tenantName) {
        FamilyMemberHistory fmh = new FamilyMemberHistory();

        // Set tenant tag
        fmh.getMeta().addTag(TENANT_TAG_SYSTEM, tenantName, "Tenant");

        // Set patient
        fmh.setPatient(new Reference("Patient/" + dto.getPatientId()));

        // Set relationship
        if (entry.getRelation() != null) {
            CodeableConcept relationship = new CodeableConcept();
            relationship.setText(entry.getRelation());
            fmh.setRelationship(relationship);
        }

        // Set condition
        if (entry.getDiagnosisCode() != null || entry.getDiagnosisText() != null) {
            FamilyMemberHistory.FamilyMemberHistoryConditionComponent condition = new FamilyMemberHistory.FamilyMemberHistoryConditionComponent();
            CodeableConcept code = new CodeableConcept();
            if (entry.getDiagnosisCode() != null) {
                code.setText(entry.getDiagnosisCode());
            }
            if (entry.getDiagnosisText() != null) {
                code.setText(entry.getDiagnosisText());
            }
            condition.setCode(code);
            fmh.addCondition(condition);
        }

        return fmh;
    }

    private FamilyHistoryDto mapFromFhir(FamilyMemberHistory fmh) {
        FamilyHistoryDto dto = new FamilyHistoryDto();

        // Extract patient ID
        if (fmh.getPatient() != null && fmh.getPatient().getReference() != null) {
            String patientRef = fmh.getPatient().getReference();
            if (patientRef.startsWith("Patient/")) {
                dto.setPatientId(Long.parseLong(patientRef.substring(8)));
            }
        }

        // For entries, this is simplistic; assume one entry per FMH
        EntryDto entry = new EntryDto();
        if (fmh.getRelationship() != null) {
            entry.setRelation(fmh.getRelationship().getText());
        }
        if (!fmh.getCondition().isEmpty()) {
            var condition = fmh.getConditionFirstRep();
            if (condition.getCode() != null) {
                entry.setDiagnosisCode(condition.getCode().getText());
            }
        }
        dto.setEntries(List.of(entry));

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
