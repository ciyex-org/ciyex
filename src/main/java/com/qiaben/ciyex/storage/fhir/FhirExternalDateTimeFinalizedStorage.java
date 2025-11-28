package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import com.qiaben.ciyex.dto.DateTimeFinalizedDto;
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
@Component("fhirExternalDateTimeFinalizedStorage")
@RequiredArgsConstructor
@Slf4j
public class FhirExternalDateTimeFinalizedStorage implements ExternalStorage<DateTimeFinalizedDto> {

    private final FhirClientProvider fhirClientProvider;

    private static final String TENANT_TAG_SYSTEM = "http://ciyex.com/tenant";

    @Override
    public String create(DateTimeFinalizedDto dto) {
        String tenantName = tenantName();
        log.info("FHIR create DateTimeFinalized for tenantName={} patientId={} encounterId={}",
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
    public void update(DateTimeFinalizedDto dto, String externalId) {
        String tenantName = tenantName();
        log.info("FHIR update DateTimeFinalized externalId={} tenantName={}", externalId, tenantName);

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
    public DateTimeFinalizedDto get(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR get DateTimeFinalized externalId={} tenantName={}", externalId, tenantName);

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

            DateTimeFinalizedDto dto = mapFromFhir(provenance);
            log.info("FHIR get success externalId={} tenantName={}", externalId, tenantName);
            return dto;
        });
    }

    @Override
    public void delete(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR delete DateTimeFinalized externalId={} tenantName={}", externalId, tenantName);

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
    public List<DateTimeFinalizedDto> searchAll() {
        // Not implemented for now
        return Collections.emptyList();
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return DateTimeFinalizedDto.class.isAssignableFrom(entityType);
    }

    private Provenance mapToFhir(DateTimeFinalizedDto dto, String tenantName) {
        Provenance provenance = new Provenance();

        // Set tenant tag
        provenance.getMeta().addTag(TENANT_TAG_SYSTEM, tenantName, "Tenant");

        // Set target (what was finalized)
        if (dto.getTargetId() != null) {
            Reference target = new Reference("Basic/" + dto.getTargetId()); // Using Basic as generic resource
            provenance.addTarget(target);
        }

        // Set agent (who finalized)
        if (dto.getFinalizedBy() != null) {
            Provenance.ProvenanceAgentComponent agent = new Provenance.ProvenanceAgentComponent();
            agent.setWho(new Reference("Practitioner/" + dto.getFinalizedBy()));
            if (dto.getFinalizerRole() != null) {
                CodeableConcept role = new CodeableConcept();
                role.setText(dto.getFinalizerRole());
                agent.setRole(Collections.singletonList(role));
            }
            provenance.addAgent(agent);
        }

        // Set occurred date/time
        if (dto.getFinalizedAt() != null) {
            try {
                provenance.setOccurred(new DateTimeType(dto.getFinalizedAt()));
            } catch (Exception e) {
                log.warn("Failed to parse finalizedAt: {}", dto.getFinalizedAt());
            }
        }

        // Set activity
        if (dto.getReason() != null) {
            CodeableConcept activity = new CodeableConcept();
            activity.setText(dto.getReason());
            provenance.setActivity(activity);
        }

        return provenance;
    }

    private DateTimeFinalizedDto mapFromFhir(Provenance provenance) {
        DateTimeFinalizedDto dto = new DateTimeFinalizedDto();

        // Extract target ID
        if (!provenance.getTarget().isEmpty()) {
            String targetRef = provenance.getTarget().get(0).getReference();
            if (targetRef.startsWith("Basic/")) {
                dto.setTargetId(Long.parseLong(targetRef.substring(6)));
            }
        }

        // Extract agent
        if (!provenance.getAgent().isEmpty()) {
            Provenance.ProvenanceAgentComponent agent = provenance.getAgent().get(0);
            if (agent.getWho() != null && agent.getWho().getReference() != null) {
                String agentRef = agent.getWho().getReference();
                if (agentRef.startsWith("Practitioner/")) {
                    dto.setFinalizedBy(agentRef.substring(12));
                }
            }
            if (!agent.getRole().isEmpty()) {
                dto.setFinalizerRole(agent.getRole().get(0).getText());
            }
        }

        // Extract occurred
        if (provenance.getOccurred() instanceof DateTimeType) {
            dto.setFinalizedAt(((DateTimeType) provenance.getOccurred()).getValueAsString());
        }

        // Extract activity
        if (provenance.getActivity() != null) {
            dto.setReason(provenance.getActivity().getText());
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
