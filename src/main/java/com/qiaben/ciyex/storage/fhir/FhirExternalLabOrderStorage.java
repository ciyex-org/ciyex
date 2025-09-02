package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import com.qiaben.ciyex.dto.LabOrderDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

@StorageType("fhir")
@Component("fhirExternalLabOrderStorage")
@Slf4j
public class FhirExternalLabOrderStorage implements ExternalStorage<LabOrderDto> {

    private final FhirClientProvider fhirClientProvider;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    @Autowired
    public FhirExternalLabOrderStorage(FhirClientProvider fhirClientProvider) {
        this.fhirClientProvider = fhirClientProvider;
    }

    @Override
    public String create(LabOrderDto dto) {
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            ServiceRequest sr = mapToServiceRequest(dto);

            var outcome = client.create()
                    .resource(sr)
                    .withAdditionalHeader("Prefer", "return=representation")
                    .execute();

            String id = (outcome != null && outcome.getId() != null) ? outcome.getId().getIdPart() : null;
            log.debug("FHIR create outcome id={}", id);
            return id; // service ignores this
        });
    }

    @Override
    public void update(LabOrderDto dto, String unused) {
        // No-op (no externalId in local model)
        executeWithRetry(() -> null);
    }

    @Override
    public LabOrderDto get(String unused) {
        // No-op
        return null;
    }

    @Override
    public void delete(String unused) {
        // No-op
    }

    @Override
    public List<LabOrderDto> searchAll() {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        Bundle bundle = fhirClientProvider.getForCurrentOrg().search()
                .forResource(ServiceRequest.class)
                .where(new TokenClientParam("_tag").exactly()
                        .systemAndCode("http://ciyex.com/tenant", orgId != null ? orgId.toString() : ""))
                .returnBundle(Bundle.class)
                .execute();

        return bundle.getEntry().stream()
                .map(entry -> mapFromServiceRequest((ServiceRequest) entry.getResource()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return LabOrderDto.class.isAssignableFrom(entityType);
    }

    // --- helpers ---

    private <T> T executeWithRetry(FhirOperation<T> op) {
        try {
            return op.execute();
        } catch (FhirClientConnectionException e) {
            if (e.getStatusCode() == 401) {
                return op.execute(); // refresh via provider
            }
            throw e;
        }
    }

    @FunctionalInterface
    private interface FhirOperation<T> { T execute(); }

    private ServiceRequest mapToServiceRequest(LabOrderDto d) {
        ServiceRequest sr = new ServiceRequest();

        // tenant tag (from RequestContext)
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        if (orgId != null) {
            sr.getMeta().addTag().setSystem("http://ciyex.com/tenant").setCode(orgId.toString());
        }

        if (d.getPatientExternalId() != null) {
            sr.setSubject(new Reference("Patient/" + d.getPatientExternalId()));
        }
        if (d.getEncounterId() != null) {
            sr.setEncounter(new Reference("Encounter/" + d.getEncounterId()));
        }

        if (d.getTestCode() != null || d.getOrderName() != null || d.getTestDisplay() != null) {
            CodeableConcept cc = new CodeableConcept();
            String display = d.getOrderName() != null ? d.getOrderName() : d.getTestDisplay();
            if (d.getTestCode() != null) {
                cc.addCoding(new Coding().setCode(d.getTestCode()).setDisplay(display));
            }
            if (display != null && cc.getCoding().isEmpty()) {
                cc.setText(display);
            }
            sr.setCode(cc);
        }

        if (d.getStatus() != null) {
            try { sr.setStatus(ServiceRequest.ServiceRequestStatus.fromCode(d.getStatus())); }
            catch (Exception ignored) { sr.setStatus(ServiceRequest.ServiceRequestStatus.DRAFT); }
        }
        if (d.getPriority() != null) {
            try { sr.setPriority(ServiceRequest.ServiceRequestPriority.fromCode(d.getPriority())); }
            catch (Exception ignored) { /* noop */ }
        }

        if (d.getOrderDateTime() != null) {
            try { sr.setAuthoredOn(DATETIME_FORMAT.parse(d.getOrderDateTime())); } catch (Exception ignored) {}
        } else if (d.getOrderDate() != null) {
            try { sr.setAuthoredOn(DATE_FORMAT.parse(d.getOrderDate())); } catch (Exception ignored) {}
        }

        if (d.getMrn() != null) sr.addIdentifier().setType(new CodeableConcept().setText("MRN")).setValue(d.getMrn());
        if (d.getOrderNumber() != null) sr.addIdentifier().setType(new CodeableConcept().setText("OrderNumber")).setValue(d.getOrderNumber());

        String requesterDisplay = d.getPhysicianName() != null ? d.getPhysicianName() : d.getOrderingProvider();
        if (requesterDisplay != null) sr.setRequester(new Reference().setDisplay(requesterDisplay));

        if (d.getLabName() != null) sr.addPerformer(new Reference().setDisplay(d.getLabName()));

        if (d.getNotes() != null) sr.addNote(new Annotation().setText(d.getNotes()));

        if (d.getSpecimenId() != null) sr.addSpecimen(new Reference("Specimen/" + d.getSpecimenId()));

        // --- NEW: icdId -> reasonCode (ICD-10-CM) ---
        if (d.getIcdId() != null && !d.getIcdId().isBlank()) {
            CodeableConcept reason = new CodeableConcept();
            reason.addCoding(
                    new Coding()
                            .setSystem("http://hl7.org/fhir/sid/icd-10-cm")
                            .setCode(d.getIcdId())
                            .setDisplay(d.getIcdId())
            );
            sr.addReasonCode(reason);
        }

        // 'result' is intentionally NOT mapped to ServiceRequest. Prefer Observation/DiagnosticReport.
        // If you still want to surface it here, you could append as a note:
        // if (d.getResult() != null) sr.addNote(new Annotation().setText("Result: " + d.getResult()));

        return sr;
    }

    private LabOrderDto mapFromServiceRequest(ServiceRequest sr) {
        LabOrderDto d = new LabOrderDto();

        if (sr.hasSubject() && sr.getSubject().hasReference()) {
            String ref = sr.getSubject().getReference(); // "Patient/{id}"
            if (ref != null && ref.startsWith("Patient/")) {
                d.setPatientExternalId(ref.substring("Patient/".length()));
            }
        }

        if (sr.hasEncounter() && sr.getEncounter().hasReference()) {
            String ref = sr.getEncounter().getReference(); // "Encounter/{id}"
            if (ref != null && ref.startsWith("Encounter/")) {
                d.setEncounterId(ref.substring("Encounter/".length()));
            }
        }

        if (sr.hasCode()) {
            CodeableConcept cc = sr.getCode();
            if (cc.hasCoding()) {
                Coding c = cc.getCodingFirstRep();
                d.setTestCode(c.getCode());
                d.setOrderName(c.getDisplay());
                d.setTestDisplay(c.getDisplay());
            } else if (cc.hasText()) {
                d.setOrderName(cc.getText());
                d.setTestDisplay(cc.getText());
            }
        }

        if (sr.hasStatus())   d.setStatus(sr.getStatus().toCode());
        if (sr.hasPriority()) d.setPriority(sr.getPriority().toCode());
        if (sr.hasAuthoredOn()) {
            d.setOrderDate(DATE_FORMAT.format(sr.getAuthoredOn()));
            d.setOrderDateTime(DATETIME_FORMAT.format(sr.getAuthoredOn()));
        }

        if (sr.hasIdentifier()) {
            for (Identifier id : sr.getIdentifier()) {
                if (id.hasType() && "MRN".equalsIgnoreCase(id.getType().getText())) d.setMrn(id.getValue());
                if (id.hasType() && "OrderNumber".equalsIgnoreCase(id.getType().getText())) d.setOrderNumber(id.getValue());
            }
        }

        if (sr.hasRequester() && sr.getRequester().hasDisplay()) {
            d.setPhysicianName(sr.getRequester().getDisplay());
            d.setOrderingProvider(sr.getRequester().getDisplay());
        }

        if (sr.hasPerformer() && !sr.getPerformer().isEmpty() && sr.getPerformerFirstRep().hasDisplay()) {
            d.setLabName(sr.getPerformerFirstRep().getDisplay());
        }

        if (sr.hasNote()) d.setNotes(sr.getNoteFirstRep().getText());

        // --- NEW: read back icdId from reasonCode ---
        if (sr.hasReasonCode()) {
            for (CodeableConcept rc : sr.getReasonCode()) {
                if (rc.hasCoding()) {
                    Coding c = rc.getCodingFirstRep();
                    if (c.getCode() != null) {
                        d.setIcdId(c.getCode());
                        break;
                    }
                } else if (rc.hasText() && d.getIcdId() == null) {
                    d.setIcdId(rc.getText());
                }
            }
        }

        // d.setResult(...) is intentionally not populated from ServiceRequest.

        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        d.setOrgId(orgId);

        return d;
    }
}
