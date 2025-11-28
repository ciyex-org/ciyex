package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import com.qiaben.ciyex.dto.PatientCodeListDto;
import com.qiaben.ciyex.entity.PatientCodeList;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalCodeListStorage;
import com.qiaben.ciyex.storage.StorageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.Callable;

@StorageType("fhir")
@Component("fhirExternalCodeListStorage")
@RequiredArgsConstructor
@Slf4j
public class FhirExternalCodeListStorage implements ExternalCodeListStorage {

    private final FhirClientProvider fhirClientProvider;

    private static final String TENANT_TAG_SYSTEM = "http://ciyex.com/tenant";

    @Override
    public String create(PatientCodeListDto dto) {
        String tenantName = tenantName();
        log.info("FHIR create PatientCodeList for tenantName={} title={}", tenantName, dto.title);

        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            Bundle bundle = mapToFhir(dto, tenantName);
            Bundle response = client.transaction().withBundle(bundle).execute();
            String externalId = response.getId() != null ? response.getId() : "CL-" + System.currentTimeMillis();
            log.info("FHIR create success externalId={} tenantName={}", externalId, tenantName);
            return externalId;
        });
    }

    @Override
    public void update(PatientCodeListDto dto, String externalId) {
        String tenantName = tenantName();
        log.info("FHIR update PatientCodeList externalId={} tenantName={}", externalId, tenantName);

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
    public PatientCodeListDto get(String externalId) {
        // Not implemented for now
        return null;
    }

    @Override
    public void delete(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR delete PatientCodeList externalId={} tenantName={}", externalId, tenantName);

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

    public List<PatientCodeListDto> searchAll(Long patientId) {
        // Not implemented
        return Collections.emptyList();
    }

    public List<PatientCodeListDto> searchAll(Long patientId, Long encounterId) {
        // Not implemented
        return Collections.emptyList();
    }

    public List<PatientCodeListDto> searchAll() {
        return Collections.emptyList();
    }

    public boolean supports(Class<?> clazz) {
        return PatientCodeListDto.class.isAssignableFrom(clazz);
    }

    private Bundle mapToFhir(PatientCodeListDto dto, String tenantName) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.TRANSACTION);

        // Create a List resource for the code list
        ListResource list = new ListResource();
        list.setStatus(ListResource.ListStatus.CURRENT);
        list.setMode(ListResource.ListMode.WORKING);
        list.setTitle(dto.title);
        if (dto.notes != null) {
            list.addNote().setText(dto.notes);
        }

        // Add codes as entries
        if (dto.codes != null && !dto.codes.trim().isEmpty()) {
            String[] codes = dto.codes.split(",");
            for (String code : codes) {
                ListResource.ListEntryComponent entry = list.addEntry();
                entry.setItem(new Reference().setDisplay(code.trim()));
            }
        }

        list.getMeta().addTag(TENANT_TAG_SYSTEM, tenantName, null);

        Bundle.BundleEntryComponent entryComp = new Bundle.BundleEntryComponent();
        entryComp.setResource(list);
        entryComp.getRequest().setMethod(Bundle.HTTPVerb.POST).setUrl("List");
        bundle.addEntry(entryComp);

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

    // Legacy methods for compatibility
    public void save(PatientCodeList patientCodeList) {
        // TODO: Map to FHIR List
        log.info("Pushing patient code list {} to FHIR", patientCodeList.getId());
    }

    public void delete(Long id) {
        // TODO: Delete from FHIR
        log.info("Deleting patient code list {} from FHIR", id);
    }

    public byte[] print(PatientCodeList patientCodeList) {
        // TODO: Generate PDF
        log.info("Printing patient code list {}", patientCodeList.getId());
        return new byte[0];
    }
}