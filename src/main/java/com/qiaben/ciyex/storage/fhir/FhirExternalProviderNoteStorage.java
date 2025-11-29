





package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import com.qiaben.ciyex.dto.ProviderNoteDto;
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
@Component("fhirExternalProviderNoteStorage")
@RequiredArgsConstructor
@Slf4j
public class FhirExternalProviderNoteStorage implements ExternalStorage<ProviderNoteDto> {

    private final FhirClientProvider fhirClientProvider;

    private static final String TENANT_TAG_SYSTEM = "http://ciyex.com/tenant";

    @Override
    public String create(ProviderNoteDto dto) {
        String tenantName = tenantName();
        log.info("FHIR create ProviderNote for tenantName={} patientId={} encounterId={}",
                tenantName, dto.getPatientId(), dto.getEncounterId());

        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            DocumentReference documentReference = mapToFhir(dto, tenantName);
            String externalId = client.create().resource(documentReference).execute().getId().getIdPart();
            log.info("FHIR create success externalId={} tenantName={}", externalId, tenantName);
            return externalId;
        });
    }

    @Override
    public void update(ProviderNoteDto dto, String externalId) {
        String tenantName = tenantName();
        log.info("FHIR update ProviderNote externalId={} tenantName={}", externalId, tenantName);

        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            DocumentReference documentReference = mapToFhir(dto, tenantName);
            documentReference.setId(externalId);
            client.update().resource(documentReference).execute();
            log.info("FHIR update success externalId={} tenantName={}", externalId, tenantName);
            return null;
        });
    }

    @Override
    public ProviderNoteDto get(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR get ProviderNote externalId={} tenantName={}", externalId, tenantName);

        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            DocumentReference documentReference = client.read().resource(DocumentReference.class).withId(externalId).execute();
            if (documentReference == null) {
                log.warn("FHIR resource not found externalId={} tenantName={}", externalId, tenantName);
                return null;
            }

            ProviderNoteDto dto = mapFromFhir(documentReference);
            log.info("FHIR get success externalId={} tenantName={}", externalId, tenantName);
            return dto;
        });
    }

    @Override
    public void delete(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR delete ProviderNote externalId={} tenantName={}", externalId, tenantName);

        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            client.delete().resourceById("DocumentReference", externalId).execute();
            log.info("FHIR delete success externalId={} tenantName={}", externalId, tenantName);
            return null;
        });
    }

    @Override
    public List<ProviderNoteDto> searchAll() {
        // Not implemented for now
        return Collections.emptyList();
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return ProviderNoteDto.class.isAssignableFrom(entityType);
    }

    private DocumentReference mapToFhir(ProviderNoteDto dto, String tenantName) {
        DocumentReference documentReference = new DocumentReference();

        // Set tenant tag
        documentReference.getMeta().addTag(TENANT_TAG_SYSTEM, tenantName, "Tenant");

        // Set subject (patient)
        documentReference.setSubject(new Reference("Patient/" + dto.getPatientId()));

        // Set context (encounter)
        if (dto.getEncounterId() != null) {
            DocumentReference.DocumentReferenceContextComponent context = new DocumentReference.DocumentReferenceContextComponent();
            context.setEncounter(Collections.singletonList(new Reference("Encounter/" + dto.getEncounterId())));
            documentReference.setContext(context);
        }

        // Set status
        documentReference.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);

        // Set type
        CodeableConcept type = new CodeableConcept();
        type.setText("Provider Note");
        documentReference.setType(type);

        // Set content
        StringBuilder content = new StringBuilder();
        if (dto.getNoteTitle() != null) content.append("Title: ").append(dto.getNoteTitle()).append("\n");
        if (dto.getSubjective() != null) content.append("Subjective: ").append(dto.getSubjective()).append("\n");
        if (dto.getObjective() != null) content.append("Objective: ").append(dto.getObjective()).append("\n");
        if (dto.getAssessment() != null) content.append("Assessment: ").append(dto.getAssessment()).append("\n");
        if (dto.getPlan() != null) content.append("Plan: ").append(dto.getPlan()).append("\n");
        if (dto.getNarrative() != null) content.append("Narrative: ").append(dto.getNarrative()).append("\n");

        DocumentReference.DocumentReferenceContentComponent contentComponent = new DocumentReference.DocumentReferenceContentComponent();
        Attachment attachment = new Attachment();
        attachment.setContentType("text/plain");
        attachment.setData(content.toString().getBytes());
        contentComponent.setAttachment(attachment);
        documentReference.addContent(contentComponent);

        return documentReference;
    }

    private ProviderNoteDto mapFromFhir(DocumentReference documentReference) {
        ProviderNoteDto dto = new ProviderNoteDto();

        // Extract patient ID
        if (documentReference.getSubject() != null && documentReference.getSubject().getReference() != null) {
            String patientRef = documentReference.getSubject().getReference();
            if (patientRef.startsWith("Patient/")) {
                dto.setPatientId(Long.parseLong(patientRef.substring(8)));
            }
        }

        // Extract encounter ID
        if (documentReference.getContext() != null && !documentReference.getContext().getEncounter().isEmpty()) {
            String encounterRef = documentReference.getContext().getEncounter().get(0).getReference();
            if (encounterRef.startsWith("Encounter/")) {
                dto.setEncounterId(Long.parseLong(encounterRef.substring(10)));
            }
        }

        // Extract content
        if (!documentReference.getContent().isEmpty()) {
            Attachment attachment = documentReference.getContentFirstRep().getAttachment();
            if (attachment != null && attachment.getData() != null) {
                String content = new String(attachment.getData());
                // Parse content back to fields (simplified)
                dto.setNarrative(content);
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

