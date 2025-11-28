package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import com.qiaben.ciyex.dto.PhysicalExamDto;
import com.qiaben.ciyex.dto.PhysicalExamSectionDto;
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
@Component("fhirExternalPhysicalExamStorage")
@RequiredArgsConstructor
@Slf4j
public class FhirExternalPhysicalExamStorage implements ExternalStorage<PhysicalExamDto> {

    private final FhirClientProvider fhirClientProvider;

    private static final String TENANT_TAG_SYSTEM = "http://ciyex.com/tenant";

    @Override
    public String create(PhysicalExamDto dto) {
        String tenantName = tenantName();
        log.info("FHIR PE create for tenantName={} patientId={} encounterId={}",
                tenantName, dto.getPatientId(), dto.getEncounterId());

        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            // Create a DiagnosticReport as the main resource for Physical Exam
            DiagnosticReport diagnosticReport = mapToFhir(dto, tenantName);
            String externalId = client.create().resource(diagnosticReport).execute().getId().getIdPart();
            log.info("FHIR create success externalId={} tenantName={}", externalId, tenantName);
            return externalId;
        });
    }

    @Override
    public void update(PhysicalExamDto dto, String externalId) {
        String tenantName = tenantName();
        log.info("FHIR PE update externalId={} tenantName={}", externalId, tenantName);

        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            DiagnosticReport diagnosticReport = mapToFhir(dto, tenantName);
            diagnosticReport.setId(externalId);
            client.update().resource(diagnosticReport).execute();
            log.info("FHIR update success externalId={} tenantName={}", externalId, tenantName);
            return null;
        });
    }

    @Override
    public PhysicalExamDto get(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR PE get externalId={} tenantName={}", externalId, tenantName);

        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            DiagnosticReport diagnosticReport = client.read().resource(DiagnosticReport.class).withId(externalId).execute();
            if (diagnosticReport == null) {
                log.warn("FHIR resource not found externalId={} tenantName={}", externalId, tenantName);
                return null;
            }

            PhysicalExamDto dto = mapFromFhir(diagnosticReport);
            log.info("FHIR get success externalId={} tenantName={}", externalId, tenantName);
            return dto;
        });
    }

    @Override
    public void delete(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR PE delete externalId={} tenantName={}", externalId, tenantName);

        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            client.delete().resourceById("DiagnosticReport", externalId).execute();
            log.info("FHIR delete success externalId={} tenantName={}", externalId, tenantName);
            return null;
        });
    }

    @Override
    public List<PhysicalExamDto> searchAll() {
        // Not implemented
        return Collections.emptyList();
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return PhysicalExamDto.class.isAssignableFrom(entityType);
    }

    private DiagnosticReport mapToFhir(PhysicalExamDto dto, String tenantName) {
        DiagnosticReport diagnosticReport = new DiagnosticReport();

        // Set tenant tag
        diagnosticReport.getMeta().addTag(TENANT_TAG_SYSTEM, tenantName, "Tenant");

        // Set subject (patient)
        diagnosticReport.setSubject(new Reference("Patient/" + dto.getPatientId()));

        // Set encounter
        if (dto.getEncounterId() != null) {
            diagnosticReport.setEncounter(new Reference("Encounter/" + dto.getEncounterId()));
        }

        // Set code for Physical Exam
        CodeableConcept code = new CodeableConcept();
        code.setText("Physical Examination");
        diagnosticReport.setCode(code);

        // Set status
        diagnosticReport.setStatus(DiagnosticReport.DiagnosticReportStatus.FINAL);

        // Set effective date
        if (dto.getAudit() != null && dto.getAudit().getCreatedDate() != null) {
            try {
                diagnosticReport.setEffective(new DateTimeType(dto.getAudit().getCreatedDate()));
            } catch (Exception e) {
                log.warn("Failed to parse created date for FHIR mapping", e);
            }
        }

        return diagnosticReport;
    }

    private PhysicalExamDto mapFromFhir(DiagnosticReport diagnosticReport) {
        PhysicalExamDto dto = new PhysicalExamDto();

        // Extract patient ID
        if (diagnosticReport.getSubject() != null && diagnosticReport.getSubject().getReference() != null) {
            String patientRef = diagnosticReport.getSubject().getReference();
            if (patientRef.startsWith("Patient/")) {
                dto.setPatientId(Long.parseLong(patientRef.substring(8)));
            }
        }

        // Extract encounter ID
        if (diagnosticReport.getEncounter() != null && diagnosticReport.getEncounter().getReference() != null) {
            String encounterRef = diagnosticReport.getEncounter().getReference();
            if (encounterRef.startsWith("Encounter/")) {
                dto.setEncounterId(Long.parseLong(encounterRef.substring(10)));
            }
        }

        // Initialize empty sections list (sections would need to be stored separately or as contained resources)
        dto.setSections(new ArrayList<>());

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
