package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import com.qiaben.ciyex.dto.ProcedureDto;
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
@Component("fhirExternalProcedureStorage")
@RequiredArgsConstructor
@Slf4j
public class FhirExternalProcedureStorage implements ExternalStorage<ProcedureDto> {

    private final FhirClientProvider fhirClientProvider;

    private static final String TENANT_TAG_SYSTEM = "http://ciyex.com/tenant";

    @Override
    public String create(ProcedureDto dto) {
        String tenantName = tenantName();
        log.info("FHIR create Procedure for tenantName={} patientId={} encounterId={} CPT={}",
                tenantName, dto.getPatientId(), dto.getEncounterId(), dto.getCpt4());

        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            org.hl7.fhir.r4.model.Procedure procedure = mapToFhir(dto, tenantName);
            String externalId = client.create().resource(procedure).execute().getId().getIdPart();
            log.info("FHIR create success externalId={} tenantName={}", externalId, tenantName);
            return externalId;
        });
    }

    @Override
    public void update(ProcedureDto dto, String externalId) {
        String tenantName = tenantName();
        log.info("FHIR update Procedure externalId={} tenantName={}", externalId, tenantName);

        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            org.hl7.fhir.r4.model.Procedure procedure = mapToFhir(dto, tenantName);
            procedure.setId(externalId);
            client.update().resource(procedure).execute();
            log.info("FHIR update success externalId={} tenantName={}", externalId, tenantName);
            return null;
        });
    }

    @Override
    public ProcedureDto get(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR get Procedure externalId={} tenantName={}", externalId, tenantName);

        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            org.hl7.fhir.r4.model.Procedure procedure = client.read().resource(org.hl7.fhir.r4.model.Procedure.class).withId(externalId).execute();
            if (procedure == null) {
                log.warn("FHIR resource not found externalId={} tenantName={}", externalId, tenantName);
                return null;
            }

            ProcedureDto dto = mapFromFhir(procedure);
            log.info("FHIR get success externalId={} tenantName={}", externalId, tenantName);
            return dto;
        });
    }

    @Override
    public void delete(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR delete Procedure externalId={} tenantName={}", externalId, tenantName);

        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            client.delete().resourceById("Procedure", externalId).execute();
            log.info("FHIR delete success externalId={} tenantName={}", externalId, tenantName);
            return null;
        });
    }

    @Override
    public List<ProcedureDto> searchAll() {
        // Not implemented for now
        return Collections.emptyList();
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return ProcedureDto.class.isAssignableFrom(entityType);
    }

    private org.hl7.fhir.r4.model.Procedure mapToFhir(ProcedureDto dto, String tenantName) {
        org.hl7.fhir.r4.model.Procedure procedure = new org.hl7.fhir.r4.model.Procedure();

        // Set tenant tag
        procedure.getMeta().addTag(TENANT_TAG_SYSTEM, tenantName, "Tenant");

        // Set subject (patient)
        procedure.setSubject(new Reference("Patient/" + dto.getPatientId()));

        // Set encounter
        if (dto.getEncounterId() != null) {
            procedure.setEncounter(new Reference("Encounter/" + dto.getEncounterId()));
        }

        // Set status
        procedure.setStatus(org.hl7.fhir.r4.model.Procedure.ProcedureStatus.COMPLETED);

        // Set code (CPT)
        if (dto.getCpt4() != null) {
            CodeableConcept code = new CodeableConcept();
            Coding coding = new Coding();
            coding.setSystem("http://www.ama-assn.org/go/cpt");
            coding.setCode(dto.getCpt4());
            coding.setDisplay(dto.getDescription());
            code.addCoding(coding);
            procedure.setCode(code);
        }

        // Set performed date/time
        if (dto.getHospitalBillingStart() != null) {
            try {
                DateTimeType performedDate = new DateTimeType(dto.getHospitalBillingStart());
                procedure.setPerformed(performedDate);
            } catch (Exception e) {
                log.warn("Invalid date format for hospitalBillingStart: {}", dto.getHospitalBillingStart());
            }
        }

        // Set note
        if (dto.getNote() != null) {
            Annotation note = new Annotation();
            note.setText(dto.getNote());
            procedure.addNote(note);
        }

        return procedure;
    }

    private ProcedureDto mapFromFhir(org.hl7.fhir.r4.model.Procedure procedure) {
        ProcedureDto dto = new ProcedureDto();

        // Extract patient ID
        if (procedure.getSubject() != null && procedure.getSubject().getReference() != null) {
            String patientRef = procedure.getSubject().getReference();
            if (patientRef.startsWith("Patient/")) {
                dto.setPatientId(Long.parseLong(patientRef.substring(8)));
            }
        }

        // Extract encounter ID
        if (procedure.getEncounter() != null && procedure.getEncounter().getReference() != null) {
            String encounterRef = procedure.getEncounter().getReference();
            if (encounterRef.startsWith("Encounter/")) {
                dto.setEncounterId(Long.parseLong(encounterRef.substring(10)));
            }
        }

        // Extract CPT code
        if (procedure.getCode() != null && !procedure.getCode().getCoding().isEmpty()) {
            Coding coding = procedure.getCode().getCoding().get(0);
            dto.setCpt4(coding.getCode());
            dto.setDescription(coding.getDisplay());
        }

        // Extract note
        if (!procedure.getNote().isEmpty()) {
            dto.setNote(procedure.getNote().get(0).getText());
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
