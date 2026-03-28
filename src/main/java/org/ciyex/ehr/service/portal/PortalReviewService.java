package org.ciyex.ehr.service.portal;

import org.ciyex.ehr.dto.portal.PortalPendingUpdateDto;
import org.ciyex.ehr.dto.portal.PortalUpdateRequest;
import org.ciyex.ehr.fhir.FhirClientService;
import org.ciyex.ehr.service.PracticeContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * FHIR-only Portal Review Service.
 * Uses FHIR Basic resource for storing pending updates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortalReviewService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;
    private final PortalNotificationService portalNotificationService;

    private static final String UPDATE_TYPE_SYSTEM = "http://ciyex.com/fhir/resource-type";
    private static final String UPDATE_TYPE_CODE = "portal-pending-update";
    private static final String EXT_USER_ID = "http://ciyex.com/fhir/StructureDefinition/user-id";
    private static final String EXT_UPDATE_TYPE = "http://ciyex.com/fhir/StructureDefinition/update-type";
    private static final String EXT_PAYLOAD = "http://ciyex.com/fhir/StructureDefinition/payload";
    private static final String EXT_HINT = "http://ciyex.com/fhir/StructureDefinition/hint";
    private static final String EXT_PRIORITY = "http://ciyex.com/fhir/StructureDefinition/priority";
    private static final String EXT_STATUS = "http://ciyex.com/fhir/StructureDefinition/status";
    private static final String EXT_PATIENT_NOTES = "http://ciyex.com/fhir/StructureDefinition/patient-notes";
    private static final String EXT_APPROVER_NOTES = "http://ciyex.com/fhir/StructureDefinition/approver-notes";
    private static final String EXT_APPROVED_BY = "http://ciyex.com/fhir/StructureDefinition/approved-by";
    private static final String EXT_REJECTION_REASON = "http://ciyex.com/fhir/StructureDefinition/rejection-reason";
    private static final String EXT_CREATED_DATE = "http://ciyex.com/fhir/StructureDefinition/created-date";
    private static final String EXT_REVIEWED_DATE = "http://ciyex.com/fhir/StructureDefinition/reviewed-date";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    public Long submitForReview(UUID userId, PortalUpdateRequest request) {
        try {
            Basic basic = new Basic();

            CodeableConcept code = new CodeableConcept();
            code.addCoding().setSystem(UPDATE_TYPE_SYSTEM).setCode(UPDATE_TYPE_CODE).setDisplay("Portal Pending Update");
            basic.setCode(code);

            basic.addExtension(new Extension(EXT_USER_ID, new StringType(userId.toString())));
            basic.addExtension(new Extension(EXT_UPDATE_TYPE, new StringType(request.getUpdateType())));
            basic.addExtension(new Extension(EXT_PAYLOAD, new StringType(mapToJson(request.getChanges()))));
            if (request.getHint() != null) {
                basic.addExtension(new Extension(EXT_HINT, new StringType(request.getHint())));
            }
            basic.addExtension(new Extension(EXT_PRIORITY, new StringType(request.getPriority() != null ? request.getPriority() : "NORMAL")));
            basic.addExtension(new Extension(EXT_STATUS, new StringType("PENDING")));
            if (request.getPatientNotes() != null) {
                basic.addExtension(new Extension(EXT_PATIENT_NOTES, new StringType(request.getPatientNotes())));
            }
            basic.addExtension(new Extension(EXT_CREATED_DATE, new DateTimeType(new Date())));

            var outcome = fhirClientService.create(basic, getPracticeId());
            String fhirId = outcome.getId().getIdPart();

            log.info("Update submitted for review - ID: {}, User: {}, Type: {}", fhirId, userId, request.getUpdateType());

            portalNotificationService.notifyReviewSubmitted(
                    "Patient " + userId.toString().substring(0, 8),
                    request.getUpdateType(),
                    request.getHint());

            return (long) Math.abs(fhirId.hashCode());

        } catch (Exception e) {
            log.error("Failed to submit update for review - User: {}, Type: {}", userId, request.getUpdateType(), e);
            throw new RuntimeException("Failed to submit update for review: " + e.getMessage());
        }
    }

    public List<PortalPendingUpdateDto> getAllPendingUpdates() {
        try {
            Bundle bundle = fhirClientService.search(Basic.class, getPracticeId());
            return fhirClientService.extractResources(bundle, Basic.class).stream()
                    .filter(this::isPendingUpdate)
                    .filter(b -> "PENDING".equals(getStringExt(b, EXT_STATUS)))
                    .map(this::toDto)
                    .sorted((a, b) -> {
                        if (a.getCreatedDate() == null) return 1;
                        if (b.getCreatedDate() == null) return -1;
                        return b.getCreatedDate().compareTo(a.getCreatedDate());
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get pending updates", e);
            throw new RuntimeException("Failed to retrieve pending updates: " + e.getMessage());
        }
    }

    public List<PortalPendingUpdateDto> getPatientUpdates(UUID userId) {
        try {
            Bundle bundle = fhirClientService.search(Basic.class, getPracticeId());
            return fhirClientService.extractResources(bundle, Basic.class).stream()
                    .filter(this::isPendingUpdate)
                    .filter(b -> userId.toString().equals(getStringExt(b, EXT_USER_ID)))
                    .map(this::toDto)
                    .sorted((a, b) -> {
                        if (a.getCreatedDate() == null) return 1;
                        if (b.getCreatedDate() == null) return -1;
                        return b.getCreatedDate().compareTo(a.getCreatedDate());
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get patient updates - User: {}", userId, e);
            throw new RuntimeException("Failed to retrieve patient updates: " + e.getMessage());
        }
    }

    public void approveUpdate(Long updateId, String approverEmail, String approverNotes) {
        try {
            Basic basic = findUpdateById(updateId);
            if (basic == null) {
                throw new IllegalArgumentException("Update not found: " + updateId);
            }

            String status = getStringExt(basic, EXT_STATUS);
            if (!"PENDING".equals(status)) {
                throw new IllegalStateException("Update is not in PENDING status: " + status);
            }

            basic.getExtension().removeIf(e -> EXT_STATUS.equals(e.getUrl()) || 
                    EXT_APPROVED_BY.equals(e.getUrl()) || EXT_APPROVER_NOTES.equals(e.getUrl()) ||
                    EXT_REVIEWED_DATE.equals(e.getUrl()));
            basic.addExtension(new Extension(EXT_STATUS, new StringType("APPROVED")));
            basic.addExtension(new Extension(EXT_APPROVED_BY, new StringType(approverEmail)));
            if (approverNotes != null) {
                basic.addExtension(new Extension(EXT_APPROVER_NOTES, new StringType(approverNotes)));
            }
            basic.addExtension(new Extension(EXT_REVIEWED_DATE, new DateTimeType(new Date())));

            fhirClientService.update(basic, getPracticeId());

            log.info("Update approved - ID: {}, Approver: {}", updateId, approverEmail);

        } catch (Exception e) {
            log.error("Failed to approve update - ID: {}, Approver: {}", updateId, approverEmail, e);
            throw new RuntimeException("Failed to approve update: " + e.getMessage());
        }
    }

    public void rejectUpdate(Long updateId, String approverEmail, String rejectionReason) {
        try {
            Basic basic = findUpdateById(updateId);
            if (basic == null) {
                throw new IllegalArgumentException("Update not found: " + updateId);
            }

            String status = getStringExt(basic, EXT_STATUS);
            if (!"PENDING".equals(status)) {
                throw new IllegalStateException("Update is not in PENDING status: " + status);
            }

            basic.getExtension().removeIf(e -> EXT_STATUS.equals(e.getUrl()) || 
                    EXT_APPROVED_BY.equals(e.getUrl()) || EXT_REJECTION_REASON.equals(e.getUrl()) ||
                    EXT_REVIEWED_DATE.equals(e.getUrl()));
            basic.addExtension(new Extension(EXT_STATUS, new StringType("REJECTED")));
            basic.addExtension(new Extension(EXT_APPROVED_BY, new StringType(approverEmail)));
            if (rejectionReason != null) {
                basic.addExtension(new Extension(EXT_REJECTION_REASON, new StringType(rejectionReason)));
            }
            basic.addExtension(new Extension(EXT_REVIEWED_DATE, new DateTimeType(new Date())));

            fhirClientService.update(basic, getPracticeId());

            log.info("Update rejected - ID: {}, Reason: {}, Approver: {}", updateId, rejectionReason, approverEmail);

        } catch (Exception e) {
            log.error("Failed to reject update - ID: {}, Approver: {}", updateId, approverEmail, e);
            throw new RuntimeException("Failed to reject update: " + e.getMessage());
        }
    }

    // -------- Helper Methods --------

    private Basic findUpdateById(Long updateId) {
        Bundle bundle = fhirClientService.search(Basic.class, getPracticeId());
        return fhirClientService.extractResources(bundle, Basic.class).stream()
                .filter(this::isPendingUpdate)
                .filter(b -> updateId.equals((long) Math.abs(b.getIdElement().getIdPart().hashCode())))
                .findFirst()
                .orElse(null);
    }

    private boolean isPendingUpdate(Basic basic) {
        if (!basic.hasCode()) return false;
        return basic.getCode().getCoding().stream()
                .anyMatch(c -> UPDATE_TYPE_SYSTEM.equals(c.getSystem()) && UPDATE_TYPE_CODE.equals(c.getCode()));
    }

    private PortalPendingUpdateDto toDto(Basic basic) {
        String fhirId = basic.getIdElement().getIdPart();

        PortalPendingUpdateDto dto = PortalPendingUpdateDto.builder()
                .id((long) Math.abs(fhirId.hashCode()))
                .updateType(getStringExt(basic, EXT_UPDATE_TYPE))
                .hint(getStringExt(basic, EXT_HINT))
                .priority(getStringExt(basic, EXT_PRIORITY))
                .status(getStringExt(basic, EXT_STATUS))
                .patientNotes(getStringExt(basic, EXT_PATIENT_NOTES))
                .approverNotes(getStringExt(basic, EXT_APPROVER_NOTES))
                .approvedBy(getStringExt(basic, EXT_APPROVED_BY))
                .rejectionReason(getStringExt(basic, EXT_REJECTION_REASON))
                .build();

        String userIdStr = getStringExt(basic, EXT_USER_ID);
        if (userIdStr != null) {
            try {
                dto.setUserId(UUID.fromString(userIdStr));
            } catch (IllegalArgumentException ignored) {}
        }

        String payloadJson = getStringExt(basic, EXT_PAYLOAD);
        if (payloadJson != null) {
            dto.setPayload(jsonToMap(payloadJson));
        }

        Extension createdExt = basic.getExtensionByUrl(EXT_CREATED_DATE);
        if (createdExt != null && createdExt.getValue() instanceof DateTimeType) {
            Date date = ((DateTimeType) createdExt.getValue()).getValue();
            if (date != null) {
                dto.setCreatedDate(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
            }
        }

        Extension reviewedExt = basic.getExtensionByUrl(EXT_REVIEWED_DATE);
        if (reviewedExt != null && reviewedExt.getValue() instanceof DateTimeType) {
            Date date = ((DateTimeType) reviewedExt.getValue()).getValue();
            if (date != null) {
                dto.setReviewedDate(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
            }
        }

        return dto;
    }

    private String getStringExt(Basic basic, String url) {
        Extension ext = basic.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType) {
            return ((StringType) ext.getValue()).getValue();
        }
        return null;
    }

    private String mapToJson(Map<String, Object> map) {
        if (map == null) return "{}";
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> jsonToMap(String json) {
        if (json == null || json.isEmpty()) return new HashMap<>();
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}
