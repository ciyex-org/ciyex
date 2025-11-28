package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import com.qiaben.ciyex.dto.AssessmentDto;
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
@Component("fhirExternalAssessmentStorage")
@RequiredArgsConstructor
@Slf4j
public class FhirExternalAssessmentStorage implements ExternalStorage<AssessmentDto> {

    private final FhirClientProvider fhirClientProvider;

    private static final String TENANT_TAG_SYSTEM = "http://ciyex.com/tenant";



    @Override
    public String create(AssessmentDto dto) {
        String tenantName = tenantName();
        log.info("FHIR create Assessment for tenantName={} patientId={} encounterId={}",
                tenantName, dto.getPatientId(), dto.getEncounterId());

        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            // For Assessment, map to Composition or DiagnosticReport
            // Placeholder: return generated ID
            String externalId = "AS-" + System.currentTimeMillis();
            log.info("FHIR create placeholder externalId={} tenantName={}", externalId, tenantName);
            return externalId;
        });
    }

    @Override
    public void update(AssessmentDto dto, String externalId) {
        String tenantName = tenantName();
        log.info("FHIR update Assessment externalId={} tenantName={}", externalId, tenantName);

        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping update.");
                return null;
            }

            // TODO: update FHIR resource
            log.info("FHIR update placeholder externalId={} tenantName={}", externalId, tenantName);
            return null;
        });
    }

    @Override
    public AssessmentDto get(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR get Assessment externalId={} tenantName={}", externalId, tenantName);

        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Cannot retrieve resource.");
                return null;
            }

            // TODO: map FHIR -> DTO
            AssessmentDto dto = new AssessmentDto();
            dto.setExternalId(externalId);
            dto.setFhirId(externalId);
            log.debug("FHIR get placeholder externalId={} tenantName={}", externalId, tenantName);
            return dto;
        });
    }

    @Override
    public void delete(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR delete Assessment externalId={} tenantName={}", externalId, tenantName);

        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping delete.");
                return null;
            }

            // TODO: delete FHIR resource
            log.info("FHIR delete placeholder externalId={} tenantName={}", externalId, tenantName);
            return null;
        });
    }

    @Override
    public List<AssessmentDto> searchAll() {
        String tenantName = tenantName();
        log.info("FHIR searchAll Assessments tenantName={}", tenantName);

        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();

            // TODO: search FHIR
            List<AssessmentDto> out = Collections.emptyList();
            log.info("FHIR searchAll placeholder found {} Assessments for tenantName={}", out.size(), tenantName);
            return out;
        });
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return AssessmentDto.class.isAssignableFrom(entityType);
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
        return Optional.ofNullable(RequestContext.get())
                .map(RequestContext::getTenantName)
                .orElse(null);
    }
}
