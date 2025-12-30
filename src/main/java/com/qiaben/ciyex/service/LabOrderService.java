package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.LabOrderDto;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * LabOrder Service - FHIR Only.
 * All lab order data is stored in HAPI FHIR server as ServiceRequest resources.
 */
@Service
@Slf4j
public class LabOrderService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    @Autowired
    public LabOrderService(FhirClientService fhirClientService, PracticeContextService practiceContextService) {
        this.fhirClientService = fhirClientService;
        this.practiceContextService = practiceContextService;
    }

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // ✅ Create lab order
    public LabOrderDto create(LabOrderDto dto) {
        validateMandatory(dto);
        log.info("Creating lab order in FHIR for patient: {}", dto.getPatientId());

        ServiceRequest fhirServiceRequest = toFhirServiceRequest(dto);
        MethodOutcome outcome = fhirClientService.create(fhirServiceRequest, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);

        log.info("Created FHIR ServiceRequest (lab order) with ID: {}", fhirId);
        return dto;
    }

    // ✅ Get one lab order
    public LabOrderDto getOne(Long id) {
        String fhirId = String.valueOf(id);
        try {
            ServiceRequest fhirServiceRequest = fhirClientService.read(ServiceRequest.class, fhirId, getPracticeId());
            return toLabOrderDto(fhirServiceRequest);
        } catch (ResourceNotFoundException e) {
            throw new IllegalArgumentException("LabOrder not found for id: " + id);
        }
    }

    // ✅ Get all lab orders
    public List<LabOrderDto> getAll() {
        log.debug("Getting all FHIR ServiceRequests (lab orders)");

        Bundle bundle = fhirClientService.getClient().search()
                .forResource(ServiceRequest.class)
                .where(new ca.uhn.fhir.rest.gclient.TokenClientParam("category").exactly()
                        .systemAndCode("http://terminology.hl7.org/CodeSystem/service-category", "laboratory"))
                .withAdditionalHeader("X-Request-Tenant-Id", getPracticeId())
                .returnBundle(Bundle.class)
                .execute();

        return extractLabOrders(bundle);
    }

    // ✅ Search all lab orders
    public ApiResponse<List<LabOrderDto>> searchAll() {
        List<LabOrderDto> data = getAll();
        return ApiResponse.<List<LabOrderDto>>builder()
                .success(true)
                .message("Lab orders retrieved successfully")
                .data(data)
                .build();
    }

    // ✅ Update lab order
    public LabOrderDto update(Long id, LabOrderDto dto) {
        String fhirId = String.valueOf(id);
        log.info("Updating FHIR ServiceRequest (lab order) with ID: {}", fhirId);

        // Validate mandatory fields
        if (dto.getOrderNumber() != null && isBlank(dto.getOrderNumber())) {
            throw new IllegalArgumentException("orderNumber cannot be blank");
        }
        if (dto.getTestCode() != null && isBlank(dto.getTestCode())) {
            throw new IllegalArgumentException("testCode cannot be blank");
        }

        ServiceRequest fhirServiceRequest = toFhirServiceRequest(dto);
        fhirServiceRequest.setId(fhirId);

        fhirClientService.update(fhirServiceRequest, getPracticeId());

        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        return dto;
    }

    // ✅ Delete lab order
    public void delete(Long id) {
        String fhirId = String.valueOf(id);
        log.info("Deleting FHIR ServiceRequest (lab order) with ID: {}", fhirId);
        fhirClientService.delete(ServiceRequest.class, fhirId, getPracticeId());
    }

    // ========== FHIR Mapping Methods ==========

    private ServiceRequest toFhirServiceRequest(LabOrderDto dto) {
        ServiceRequest sr = new ServiceRequest();

        // Category: laboratory
        sr.addCategory()
                .addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/service-category")
                .setCode("laboratory")
                .setDisplay("Laboratory");

        // Subject (Patient)
        if (dto.getPatientId() != null) {
            sr.setSubject(new Reference("Patient/" + dto.getPatientId()));
        }

        // Status
        sr.setStatus(ServiceRequest.ServiceRequestStatus.ACTIVE);

        // Intent
        sr.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);

        // Code (test code)
        if (dto.getTestCode() != null) {
            sr.setCode(new CodeableConcept()
                    .addCoding(new Coding().setCode(dto.getTestCode()))
                    .setText(dto.getTestDisplay()));
        }

        // Identifier (order number)
        if (dto.getOrderNumber() != null) {
            sr.addIdentifier()
                    .setSystem("http://hospital.example.org/order-number")
                    .setValue(dto.getOrderNumber());
        }

        // Requester (ordering provider)
        if (dto.getOrderingProvider() != null) {
            sr.setRequester(new Reference().setDisplay(dto.getOrderingProvider()));
        }

        // Performer (physician)
        if (dto.getPhysicianName() != null) {
            sr.addPerformer(new Reference().setDisplay(dto.getPhysicianName()));
        }

        // Priority
        if (dto.getPriority() != null) {
            try {
                sr.setPriority(ServiceRequest.ServiceRequestPriority.fromCode(dto.getPriority().toLowerCase()));
            } catch (Exception e) {
                sr.setPriority(ServiceRequest.ServiceRequestPriority.ROUTINE);
            }
        }

        // Reason code (diagnosis)
        if (dto.getDiagnosisCode() != null) {
            sr.addReasonCode(new CodeableConcept()
                    .addCoding(new Coding().setCode(dto.getDiagnosisCode())));
        }

        // Notes
        if (dto.getNotes() != null) {
            sr.addNote().setText(dto.getNotes());
        }

        // Occurrence (order date)
        if (dto.getOrderDate() != null) {
            sr.setOccurrence(new DateTimeType(dto.getOrderDate()));
        }

        return sr;
    }

    private LabOrderDto toLabOrderDto(ServiceRequest sr) {
        LabOrderDto dto = new LabOrderDto();

        // FHIR ID
        if (sr.hasId()) {
            dto.setFhirId(sr.getIdElement().getIdPart());
            dto.setExternalId(sr.getIdElement().getIdPart());
        }

        // Patient ID
        if (sr.hasSubject() && sr.getSubject().hasReference()) {
            String ref = sr.getSubject().getReference();
            if (ref.startsWith("Patient/")) {
                try {
                    dto.setPatientId(Long.parseLong(ref.substring(8)));
                } catch (NumberFormatException e) {
                    // Non-numeric FHIR ID
                }
            }
        }

        // Test code
        if (sr.hasCode()) {
            if (sr.getCode().hasCoding()) {
                dto.setTestCode(sr.getCode().getCodingFirstRep().getCode());
            }
            dto.setTestDisplay(sr.getCode().getText());
        }

        // Order number
        if (sr.hasIdentifier()) {
            dto.setOrderNumber(sr.getIdentifierFirstRep().getValue());
        }

        // Ordering provider
        if (sr.hasRequester()) {
            dto.setOrderingProvider(sr.getRequester().getDisplay());
        }

        // Physician
        if (sr.hasPerformer()) {
            dto.setPhysicianName(sr.getPerformerFirstRep().getDisplay());
        }

        // Priority
        if (sr.hasPriority()) {
            dto.setPriority(sr.getPriority().toCode());
        }

        // Status
        if (sr.hasStatus()) {
            dto.setStatus(sr.getStatus().toCode());
        }

        // Diagnosis code
        if (sr.hasReasonCode()) {
            dto.setDiagnosisCode(sr.getReasonCodeFirstRep().getCodingFirstRep().getCode());
        }

        // Notes
        if (sr.hasNote()) {
            dto.setNotes(sr.getNoteFirstRep().getText());
        }

        // Order date
        if (sr.hasOccurrenceDateTimeType()) {
            dto.setOrderDate(sr.getOccurrenceDateTimeType().getValueAsString());
        }

        return dto;
    }

    private List<LabOrderDto> extractLabOrders(Bundle bundle) {
        List<LabOrderDto> items = new ArrayList<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof ServiceRequest) {
                    items.add(toLabOrderDto((ServiceRequest) entry.getResource()));
                }
            }
        }
        return items;
    }

    // ========== Validation Helpers ==========

    private void validateMandatory(LabOrderDto dto) {
        if (dto == null) throw new IllegalArgumentException("lab order payload is required");
        if (isBlank(dto.getOrderNumber())) throw new IllegalArgumentException("orderNumber is required");
        if (isBlank(dto.getTestCode())) throw new IllegalArgumentException("testCode is required");
        if (isBlank(dto.getPhysicianName())) throw new IllegalArgumentException("physicianName is required");
        if (isBlank(dto.getOrderingProvider())) throw new IllegalArgumentException("orderingProvider is required");
        if (isBlank(dto.getDiagnosisCode())) throw new IllegalArgumentException("diagnosisCode is required");
        if (isBlank(dto.getProcedureCode())) throw new IllegalArgumentException("procedureCode is required");
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
