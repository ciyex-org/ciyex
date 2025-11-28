package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import com.qiaben.ciyex.dto.VitalsDto;
import com.qiaben.ciyex.entity.Vitals;
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
@Component("fhirExternalVitalsStorage")
@RequiredArgsConstructor
@Slf4j
public class FhirExternalVitalsStorage implements ExternalStorage<VitalsDto> {

    private final FhirClientProvider fhirClientProvider;

    private static final String TENANT_TAG_SYSTEM = "http://ciyex.com/tenant";

    @Override
    public String create(VitalsDto dto) {
        String tenantName = tenantName();
        log.info("FHIR create Vitals for tenantName={} patientId={} encounterId={}",
                tenantName, dto.getPatientId(), dto.getEncounterId());

        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null - FHIR not configured. Skipping external storage sync.");
                return null;
            }

            Bundle bundle = mapToFhir(dto, tenantName);
            Bundle response = client.transaction().withBundle(bundle).execute();
            String externalId = response.getId() != null ? response.getId() : "V-" + System.currentTimeMillis();
            log.info("FHIR create success externalId={} tenantName={}", externalId, tenantName);
            return externalId;
        });
    }

    @Override
    public void update(VitalsDto dto, String externalId) {
        String tenantName = tenantName();
        log.info("FHIR update Vitals externalId={} tenantName={}", externalId, tenantName);

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
    public VitalsDto get(String externalId) {
        // Not implemented for now
        return null;
    }

    @Override
    public void delete(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR delete Vitals externalId={} tenantName={}", externalId, tenantName);

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

    public List<VitalsDto> searchAll(Long patientId) {
        // Not implemented
        return Collections.emptyList();
    }

    public List<VitalsDto> searchAll(Long patientId, Long encounterId) {
        // Not implemented
        return Collections.emptyList();
    }

    public List<VitalsDto> searchAll() {
        return Collections.emptyList();
    }

    public boolean supports(Class<?> clazz) {
        return VitalsDto.class.isAssignableFrom(clazz);
    }

    private Bundle mapToFhir(VitalsDto dto, String tenantName) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.TRANSACTION);

        // Create Observations for each vital sign
        if (dto.getWeightKg() != null) {
            addObservation(bundle, dto, "weight", "Weight", new Quantity().setValue(dto.getWeightKg()).setUnit("kg"), tenantName);
        }
        if (dto.getHeightCm() != null) {
            addObservation(bundle, dto, "height", "Height", new Quantity().setValue(dto.getHeightCm()).setUnit("cm"), tenantName);
        }
        if (dto.getBpSystolic() != null && dto.getBpDiastolic() != null) {
            addObservation(bundle, dto, "blood-pressure", "Blood Pressure", new Quantity().setValue(dto.getBpSystolic()).setUnit("mmHg"), tenantName);
        }
        if (dto.getPulse() != null) {
            addObservation(bundle, dto, "heart-rate", "Heart Rate", new Quantity().setValue(dto.getPulse()).setUnit("bpm"), tenantName);
        }
        if (dto.getRespiration() != null) {
            addObservation(bundle, dto, "respiratory-rate", "Respiratory Rate", new Quantity().setValue(dto.getRespiration()).setUnit("/min"), tenantName);
        }
        if (dto.getTemperatureC() != null) {
            addObservation(bundle, dto, "body-temperature", "Body Temperature", new Quantity().setValue(dto.getTemperatureC()).setUnit("C"), tenantName);
        }
        if (dto.getOxygenSaturation() != null) {
            addObservation(bundle, dto, "oxygen-saturation", "Oxygen Saturation", new Quantity().setValue(dto.getOxygenSaturation()).setUnit("%"), tenantName);
        }

        return bundle;
    }

    private void addObservation(Bundle bundle, VitalsDto dto, String code, String display, Quantity value, String tenantName) {
        Observation obs = new Observation();
        obs.setStatus(Observation.ObservationStatus.FINAL);
        obs.getCategoryFirstRep().addCoding()
            .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
            .setCode("vital-signs")
            .setDisplay("Vital Signs");

        obs.getCode().addCoding()
            .setSystem("http://loinc.org")
            .setCode(code)
            .setDisplay(display);

        obs.setValue(value);

        obs.setSubject(new Reference("Patient/" + dto.getPatientId()));
        obs.setEncounter(new Reference("Encounter/" + dto.getEncounterId()));

        obs.getMeta().addTag(TENANT_TAG_SYSTEM, tenantName, null);

        Bundle.BundleEntryComponent entryComp = new Bundle.BundleEntryComponent();
        entryComp.setResource(obs);
        entryComp.getRequest().setMethod(Bundle.HTTPVerb.POST).setUrl("Observation");
        bundle.addEntry(entryComp);
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
    public void save(Vitals vitals) {
        // TODO: Map to FHIR Observation
        log.info("Pushing vitals {} to FHIR", vitals.getId());
    }

    public void delete(Long id) {
        // TODO: Delete from FHIR
        log.info("Deleting vitals {} from FHIR", id);
    }

    public byte[] print(Vitals vitals) {
        // TODO: Generate PDF
        log.info("Printing vitals {}", vitals.getId());
        return new byte[0];
    }
}
