package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import com.qiaben.ciyex.dto.SignoffDto;
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
@Component("fhirExternalSignoffStorage")
@RequiredArgsConstructor
@Slf4j
public class FhirExternalSignoffStorage implements ExternalStorage<SignoffDto> {

    private final FhirClientProvider fhirClientProvider;

    private static final String TENANT_TAG_SYSTEM = "http://ciyex.com/tenant";

    @Override
    public String create(SignoffDto dto) {
        String tenantName = tenantName();
        log.info("FHIR create Signoff for tenantName={} patientId={} encounterId={} targetType={} targetId={}",
                tenantName, dto.getPatientId(), dto.getEncounterId(), dto.getTargetType(), dto.getTargetId());

        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            Provenance provenance = mapToFhir(dto, tenantName);
            String externalId = client.create().resource(provenance).execute().getId().getIdPart();
            log.info("FHIR create success externalId={} tenantName={}", externalId, tenantName);
            return externalId;
        });
    }

    @Override
    public void update(SignoffDto dto, String externalId) {
        String tenantName = tenantName();
        log.info("FHIR update Signoff externalId={} tenantName={}", externalId, tenantName);

        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            Provenance provenance = mapToFhir(dto, tenantName);
            provenance.setId(externalId);
            client.update().resource(provenance).execute();
            log.info("FHIR update success externalId={} tenantName={}", externalId, tenantName);
            return null;
        });
    }

    @Override
    public SignoffDto get(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR get Signoff externalId={} tenantName={}", externalId, tenantName);

        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            Provenance provenance = client.read().resource(Provenance.class).withId(externalId).execute();
            if (provenance == null) {
                log.warn("FHIR resource not found externalId={} tenantName={}", externalId, tenantName);
                return null;
            }

            SignoffDto dto = mapFromFhir(provenance);
            log.info("FHIR get success externalId={} tenantName={}", externalId, tenantName);
            return dto;
        });
    }

    @Override
    public void delete(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR delete Signoff externalId={} tenantName={}", externalId, tenantName);

        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            client.delete().resourceById("Provenance", externalId).execute();
            log.info("FHIR delete success externalId={} tenantName={}", externalId, tenantName);
            return null;
        });
    }

    @Override
    public List<SignoffDto> searchAll() {
        // Not implemented for Signoff
        return Collections.emptyList();
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return SignoffDto.class.isAssignableFrom(entityType);
    }

    private Provenance mapToFhir(SignoffDto dto, String tenantName) {
        Provenance provenance = new Provenance();
        provenance.setRecorded(new Date()); // Current date

        // Set target (what is being signed)
        if (dto.getTargetId() != null) {
            Reference targetRef = new Reference();
            // Assuming target is an Encounter or other resource
            targetRef.setReference("Encounter/" + dto.getEncounterId()); // Or based on targetType
            provenance.addTarget(targetRef);
        }

        // Set agent (who signed)
        if (dto.getSignedBy() != null) {
            Provenance.ProvenanceAgentComponent agent = new Provenance.ProvenanceAgentComponent();
            agent.setWho(new Reference("Practitioner/" + dto.getSignedBy())); // Or appropriate reference
            agent.setRole(Collections.singletonList(new CodeableConcept().setText(dto.getSignerRole())));
            provenance.addAgent(agent);
        }

        // Set signature
        if (dto.getSignatureData() != null) {
            Signature signature = new Signature();
            signature.setType(Collections.singletonList(new Coding().setSystem("urn:iso-astm:E1762-95:2013").setCode("1.2.840.10065.1.12.1.1"))); // Author signature
            signature.setWhen(new Date()); // Signed at
            signature.setWho(new Reference("Practitioner/" + dto.getSignedBy()));
            signature.setData(dto.getSignatureData().getBytes()); // Base64 or data
            provenance.addSignature(signature);
        }

        // Set activity based on status
        if ("Signed".equals(dto.getStatus())) {
            provenance.setActivity(new CodeableConcept().setText("signed"));
        }

        // Add tenant tag
        provenance.getMeta().addTag(TENANT_TAG_SYSTEM, tenantName, "Tenant");

        return provenance;
    }

    private SignoffDto mapFromFhir(Provenance provenance) {
        SignoffDto dto = new SignoffDto();

        // Extract patient and encounter if possible
        // This might need adjustment based on how targets are set

        // Extract signed by
        if (!provenance.getAgent().isEmpty()) {
            Provenance.ProvenanceAgentComponent agent = provenance.getAgent().get(0);
            if (agent.getWho() != null && agent.getWho().getReference() != null) {
                String ref = agent.getWho().getReference();
                if (ref.startsWith("Practitioner/")) {
                    dto.setSignedBy(ref.substring(12));
                }
            }
            if (!agent.getRole().isEmpty()) {
                dto.setSignerRole(agent.getRole().get(0).getText());
            }
        }

        // Extract signature
        if (!provenance.getSignature().isEmpty()) {
            Signature signature = provenance.getSignature().get(0);
            dto.setSignatureData(new String(signature.getData())); // Assuming it's text
        }

        // Set status based on activity
        if (provenance.getActivity() != null) {
            dto.setStatus("Signed");
        } else {
            dto.setStatus("Draft");
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
