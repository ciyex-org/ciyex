package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import com.qiaben.ciyex.dto.ProviderSignatureDto;
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
@Component("fhirExternalProviderSignatureStorage")
@RequiredArgsConstructor
@Slf4j
public class FhirExternalProviderSignatureStorage implements ExternalStorage<ProviderSignatureDto> {

    private final FhirClientProvider fhirClientProvider;

    private static final String TENANT_TAG_SYSTEM = "http://ciyex.com/tenant";

    @Override
    public String create(ProviderSignatureDto dto) {
        String tenantName = tenantName();
        log.info("FHIR create ProviderSignature for tenantName={} patientId={} encounterId={}",
                tenantName, dto.getPatientId(), dto.getEncounterId());

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
    public void update(ProviderSignatureDto dto, String externalId) {
        String tenantName = tenantName();
        log.info("FHIR update ProviderSignature externalId={} tenantName={}", externalId, tenantName);

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
    public ProviderSignatureDto get(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR get ProviderSignature externalId={} tenantName={}", externalId, tenantName);

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

            ProviderSignatureDto dto = mapFromFhir(provenance);
            log.info("FHIR get success externalId={} tenantName={}", externalId, tenantName);
            return dto;
        });
    }

    @Override
    public void delete(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR delete ProviderSignature externalId={} tenantName={}", externalId, tenantName);

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
    public List<ProviderSignatureDto> searchAll() {
        // Not implemented for now
        return Collections.emptyList();
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return ProviderSignatureDto.class.isAssignableFrom(entityType);
    }

    private Provenance mapToFhir(ProviderSignatureDto dto, String tenantName) {
        Provenance provenance = new Provenance();

        // Set tenant tag
        provenance.getMeta().addTag(TENANT_TAG_SYSTEM, tenantName, "Tenant");

        // Set target (encounter)
        provenance.addTarget(new Reference("Encounter/" + dto.getEncounterId()));

        // Set recorded date
        if (dto.getSignedAt() != null) {
            provenance.setRecorded(Date.from(java.time.Instant.parse(dto.getSignedAt())));
        }

        // Set agent (signer)
        Provenance.ProvenanceAgentComponent agent = new Provenance.ProvenanceAgentComponent();
        agent.setWho(new Reference("Practitioner/" + dto.getSignedBy()));
        if (dto.getSignerRole() != null) {
            CodeableConcept role = new CodeableConcept();
            role.setText(dto.getSignerRole());
            agent.setRole(Collections.singletonList(role));
        }
        provenance.addAgent(agent);

        // Set signature
        if (dto.getSignatureData() != null) {
            Signature signature = new Signature();
            signature.setType(Collections.singletonList(new Coding("urn:ietf:rfc:3986", "1.2.840.10065.1.12.1.1", "Author's Signature")));
            signature.setWhen(Date.from(java.time.Instant.now()));
            signature.setWho(new Reference("Practitioner/" + dto.getSignedBy()));
            signature.setData(dto.getSignatureData().getBytes());
            provenance.addSignature(signature);
        }

        return provenance;
    }

    private ProviderSignatureDto mapFromFhir(Provenance provenance) {
        ProviderSignatureDto dto = new ProviderSignatureDto();

        // Extract encounter ID
        if (!provenance.getTarget().isEmpty()) {
            String targetRef = provenance.getTarget().get(0).getReference();
            if (targetRef.startsWith("Encounter/")) {
                dto.setEncounterId(Long.parseLong(targetRef.substring(10)));
            }
        }

        // Extract signer
        if (!provenance.getAgent().isEmpty()) {
            Provenance.ProvenanceAgentComponent agent = provenance.getAgent().get(0);
            if (agent.getWho() != null) {
                String whoRef = agent.getWho().getReference();
                if (whoRef.startsWith("Practitioner/")) {
                    dto.setSignedBy(whoRef.substring(12));
                }
            }
            if (!agent.getRole().isEmpty()) {
                dto.setSignerRole(agent.getRole().get(0).getText());
            }
        }

        // Extract signature
        if (!provenance.getSignature().isEmpty()) {
            Signature sig = provenance.getSignature().get(0);
            dto.setSignatureData(new String(sig.getData()));
            dto.setSignedAt(sig.getWhen().toString());
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
