package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import com.qiaben.ciyex.dto.LabResultDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.entity.LabResult;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.repository.LabResultRepository;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.StorageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * FHIR storage adapter for LabResult -> Observation.
 * Persists the Observation id into LabResult.externalId.
 */
@StorageType("fhir")
@Component("fhirExternalLabResultStorage")
@RequiredArgsConstructor
@Slf4j
public class FhirExternalLabResultStorage implements ExternalStorage<LabResultDto> {

    private final FhirClientProvider fhirClientProvider;
    private final LabResultRepository labResultRepository;
    private static final String TENANT_TAG_SYSTEM = "http://ciyex.com/tenant";

    @Override
    @Transactional
    public String create(LabResultDto dto) {
        String tenantName = tenantName();
        log.info("FHIR create LabResult Observation tenantName={} patientId={} testName={}", tenantName, dto.getPatientId(), dto.getTestName());
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            Observation obs = mapToObservation(dto, tenantName);
            String externalId = client.create().resource(obs).execute().getId().getIdPart();
            // persist external id
            LabResult entity = labResultRepository.findById(dto.getId() == null ? -1L : dto.getId()).orElse(null);
            if (entity != null) {
                entity.setExternalId(externalId);
                labResultRepository.save(entity);
            }
            log.info("FHIR create LabResult Observation success externalId={}", externalId);
            return externalId;
        });
    }

    @Override
    @Transactional
    public void update(LabResultDto dto, String externalId) {
        String tenantName = tenantName();
        log.info("FHIR update LabResult Observation externalId={} tenantName={}", externalId, tenantName);
        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            Observation obs = mapToObservation(dto, tenantName);
            obs.setId(externalId);
            client.update().resource(obs).execute();
            LabResult entity = labResultRepository.findById(dto.getId() == null ? -1L : dto.getId()).orElse(null);
            if (entity != null && (entity.getExternalId() == null || !entity.getExternalId().equals(externalId))) {
                entity.setExternalId(externalId);
                labResultRepository.save(entity);
            }
            return null;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public LabResultDto get(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR get LabResult Observation externalId={} tenantName={}", externalId, tenantName);
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            Observation obs = client.read().resource(Observation.class).withId(externalId).execute();
            LabResultDto dto = mapFromObservation(obs);
            dto.setExternalId(externalId);
            return dto;
        });
    }

    @Override
    public void delete(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR delete LabResult Observation externalId={} tenantName={}", externalId, tenantName);
        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            client.delete().resourceById("Observation", externalId).execute();
            // clear local linkage if present
            labResultRepository.findAll().stream().filter(r -> externalId.equals(r.getExternalId())).forEach(r -> {
                r.setExternalId(null);
                labResultRepository.save(r);
            });
            return null;
        });
    }

    @Override
    public List<LabResultDto> searchAll() {
        String tenantName = tenantName();
        log.info("FHIR searchAll LabResult Observations tenantName={}", tenantName);
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            Bundle bundle = client.search()
                    .forResource(Observation.class)
                    .where(new TokenClientParam("_tag").exactly().systemAndCode(TENANT_TAG_SYSTEM, tenantName != null ? tenantName : ""))
                    .returnBundle(Bundle.class)
                    .execute();

            final List<Bundle.BundleEntryComponent> entries = new ArrayList<>(bundle.getEntry());
            while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
                bundle = client.loadPage().next(bundle).execute();
                entries.addAll(bundle.getEntry());
            }
            return entries.stream()
                    .map(e -> (Observation) e.getResource())
                    .map(obs -> {
                        LabResultDto dto = mapFromObservation(obs);
                        dto.setExternalId(obs.getIdElement().getIdPart());
                        return dto;
                    })
                    .collect(Collectors.toList());
        });
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return LabResultDto.class.isAssignableFrom(entityType);
    }

    // --- mapping ---
    private Observation mapToObservation(LabResultDto d, String tenantName) {
        Observation obs = new Observation();
        obs.setStatus(Observation.ObservationStatus.FINAL); // default; could map d.status
        if (d.getStatus() != null) {
            try { obs.setStatus(Observation.ObservationStatus.valueOf(d.getStatus().toUpperCase())); } catch (Exception ignored) { }
        }
        // Code
        CodeableConcept code = new CodeableConcept();
        if (d.getCode() != null) {
            code.addCoding().setSystem("http://loinc.org").setCode(d.getCode()).setDisplay(d.getTestName());
        } else {
            code.setText(d.getTestName());
        }
        obs.setCode(code);

        // Subject reference (patient)
        if (d.getPatientId() != null) {
            obs.setSubject(new Reference("Patient/" + d.getPatientId()));
        }

        // Effective date/time
        if (d.getResultDate() != null) {
            obs.setEffective(new DateTimeType(d.getResultDate()));
        }

        // Value
        if (d.getValue() != null) {
            try {
                BigDecimal val = new BigDecimal(d.getValue());
                Quantity q = new Quantity();
                q.setValue(val);
                q.setUnit(d.getUnits());
                obs.setValue(q);
            } catch (NumberFormatException nfe) {
                obs.setValue(new CodeableConcept().setText(d.getValue()));
            }
        }

        // Interpretation (abnormalFlag)
        if (d.getAbnormalFlag() != null) {
            obs.addInterpretation(new CodeableConcept().setText(d.getAbnormalFlag()));
        }

        // Reference range
        if (d.getReferenceRange() != null) {
            Observation.ObservationReferenceRangeComponent rr = new Observation.ObservationReferenceRangeComponent();
            rr.setText(d.getReferenceRange());
            obs.addReferenceRange(rr);
        }

        // Notes / recommendations
        if (d.getRecommendations() != null) {
            obs.addNote(new Annotation().setText(d.getRecommendations()));
        }

        // Specimen reference if provided (use id directly for now)
        if (d.getSpecimen() != null) {
            obs.setSpecimen(new Reference("Specimen/" + d.getSpecimen()));
        }

        // Tag tenant
        obs.getMeta().addTag().setSystem(TENANT_TAG_SYSTEM).setCode(tenantName != null ? tenantName : "");
        return obs;
    }

    private LabResultDto mapFromObservation(Observation obs) {
        LabResultDto d = new LabResultDto();
        if (obs.getCode() != null) {
            if (obs.getCode().hasCoding()) {
                Coding c = obs.getCode().getCodingFirstRep();
                d.setCode(c.getCode());
                d.setTestName(c.getDisplay());
            } else {
                d.setTestName(obs.getCode().getText());
            }
        }
        if (obs.getSubject() != null && obs.getSubject().getReferenceElement().hasIdPart()) {
            try { d.setPatientId(Long.parseLong(obs.getSubject().getReferenceElement().getIdPart())); } catch (NumberFormatException ignored) { }
        }
        if (obs.getEffectiveDateTimeType() != null) {
            d.setResultDate(obs.getEffectiveDateTimeType().getValueAsString());
        }
        if (obs.hasValue()) {
            if (obs.getValue() instanceof Quantity q) {
                if (q.getValue() != null) d.setValue(q.getValue().toPlainString());
                d.setUnits(q.getUnit());
            } else if (obs.getValue() instanceof CodeableConcept cc) {
                d.setValue(cc.getText());
            }
        }
        if (obs.hasInterpretation()) {
            d.setAbnormalFlag(obs.getInterpretationFirstRep().getText());
        }
        if (obs.hasReferenceRange()) {
            d.setReferenceRange(obs.getReferenceRangeFirstRep().getText());
        }
        if (obs.hasNote()) {
            d.setRecommendations(obs.getNoteFirstRep().getText());
        }
        // speciment reference id part -> specimen string
        if (obs.getSpecimen() != null && obs.getSpecimen().getReferenceElement().hasIdPart()) {
            d.setSpecimen(obs.getSpecimen().getReferenceElement().getIdPart());
        }
        return d;
    }

    // --- utility ---
    private <T> T executeWithRetry(Callable<T> op) {
        try {
            return op.call();
        } catch (FhirClientConnectionException e) {
            log.error("FHIR LabResult connection error status={} msg={}", e.getStatusCode(), e.getMessage());
            if (e.getStatusCode() == 401) {
                log.warn("LabResult 401 unauthorized; retrying once");
                try { return op.call(); } catch (Exception ex) { throw new RuntimeException(ex); }
            }
            throw e;
        } catch (Exception e) {
            log.error("Unexpected FHIR LabResult error msg={}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private String tenantName() {
        return RequestContext.get() != null ? RequestContext.get().getTenantName() : null;
    }
}
