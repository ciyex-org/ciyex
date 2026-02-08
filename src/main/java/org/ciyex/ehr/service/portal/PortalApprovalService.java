package org.ciyex.ehr.service.portal;

import org.ciyex.ehr.dto.portal.ApiResponse;
import org.ciyex.ehr.dto.portal.PortalUserDto;
import org.ciyex.ehr.fhir.FhirClientService;
import org.ciyex.ehr.service.PracticeContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR-only Portal Approval Service.
 * Uses FHIR Person resource for portal users and Patient resource for EHR patients.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalApprovalService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private static final String PORTAL_USER_SYSTEM = "http://ciyex.com/fhir/resource-type";
    private static final String PORTAL_USER_CODE = "portal-user";
    private static final String EXT_STATUS = "http://ciyex.com/fhir/StructureDefinition/portal-status";
    private static final String EXT_APPROVED_DATE = "http://ciyex.com/fhir/StructureDefinition/approved-date";
    private static final String EXT_APPROVED_BY = "http://ciyex.com/fhir/StructureDefinition/approved-by";
    private static final String EXT_REJECTED_DATE = "http://ciyex.com/fhir/StructureDefinition/rejected-date";
    private static final String EXT_REJECTED_BY = "http://ciyex.com/fhir/StructureDefinition/rejected-by";
    private static final String EXT_REJECTION_REASON = "http://ciyex.com/fhir/StructureDefinition/rejection-reason";
    private static final String EXT_EHR_PATIENT_ID = "http://ciyex.com/fhir/StructureDefinition/ehr-patient-id";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    public ApiResponse<List<PortalUserDto>> getPendingUsers() {
        try {
            Bundle bundle = fhirClientService.search(Person.class, getPracticeId());
            List<PortalUserDto> pendingUsers = fhirClientService.extractResources(bundle, Person.class).stream()
                    .filter(this::isPortalUser)
                    .filter(p -> "PENDING".equals(getStringExt(p, EXT_STATUS)))
                    .map(this::toPortalUserDto)
                    .collect(Collectors.toList());

            return ApiResponse.<List<PortalUserDto>>builder()
                    .success(true)
                    .message("Pending users retrieved successfully")
                    .data(pendingUsers)
                    .build();
        } catch (Exception e) {
            log.error("Error retrieving pending users", e);
            return ApiResponse.<List<PortalUserDto>>builder()
                    .success(false)
                    .message("Failed to retrieve pending users")
                    .build();
        }
    }

    public ApiResponse<PortalUserDto> approveUser(Long portalUserId, Long approvedByUserId) {
        try {
            String fhirId = String.valueOf(portalUserId);
            Person person = fhirClientService.read(Person.class, fhirId, getPracticeId());

            if (person == null) {
                return ApiResponse.<PortalUserDto>builder()
                        .success(false)
                        .message("Portal user not found")
                        .build();
            }

            String status = getStringExt(person, EXT_STATUS);
            if (!"PENDING".equals(status)) {
                return ApiResponse.<PortalUserDto>builder()
                        .success(false)
                        .message("User is not in pending status")
                        .build();
            }

            // Create EHR Patient from portal user
            Patient ehrPatient = createEhrPatient(person);
            var outcome = fhirClientService.create(ehrPatient, getPracticeId());
            String ehrPatientId = outcome.getId().getIdPart();

            // Update portal user status
            person.getExtension().removeIf(e -> EXT_STATUS.equals(e.getUrl()) || 
                    EXT_APPROVED_DATE.equals(e.getUrl()) || EXT_APPROVED_BY.equals(e.getUrl()) ||
                    EXT_EHR_PATIENT_ID.equals(e.getUrl()));
            person.addExtension(new Extension(EXT_STATUS, new StringType("APPROVED")));
            person.addExtension(new Extension(EXT_APPROVED_DATE, new DateTimeType(new Date())));
            person.addExtension(new Extension(EXT_APPROVED_BY, new StringType(String.valueOf(approvedByUserId))));
            person.addExtension(new Extension(EXT_EHR_PATIENT_ID, new StringType(ehrPatientId)));

            fhirClientService.update(person, getPracticeId());

            log.info("Portal user approved successfully: {} -> EHR Patient ID: {}", fhirId, ehrPatientId);

            return ApiResponse.<PortalUserDto>builder()
                    .success(true)
                    .message("Portal user approved and synced to EHR")
                    .data(toPortalUserDto(person))
                    .build();

        } catch (Exception e) {
            log.error("Error approving portal user: {}", portalUserId, e);
            return ApiResponse.<PortalUserDto>builder()
                    .success(false)
                    .message("Failed to approve user: " + e.getMessage())
                    .build();
        }
    }

    public ApiResponse<PortalUserDto> rejectUser(Long portalUserId, String reason, Long rejectedByUserId) {
        try {
            String fhirId = String.valueOf(portalUserId);
            Person person = fhirClientService.read(Person.class, fhirId, getPracticeId());

            if (person == null) {
                return ApiResponse.<PortalUserDto>builder()
                        .success(false)
                        .message("Portal user not found")
                        .build();
            }

            String status = getStringExt(person, EXT_STATUS);
            if (!"PENDING".equals(status)) {
                return ApiResponse.<PortalUserDto>builder()
                        .success(false)
                        .message("User is not in pending status")
                        .build();
            }

            // Update portal user status
            person.getExtension().removeIf(e -> EXT_STATUS.equals(e.getUrl()) || 
                    EXT_REJECTED_DATE.equals(e.getUrl()) || EXT_REJECTED_BY.equals(e.getUrl()) ||
                    EXT_REJECTION_REASON.equals(e.getUrl()));
            person.addExtension(new Extension(EXT_STATUS, new StringType("REJECTED")));
            person.addExtension(new Extension(EXT_REJECTED_DATE, new DateTimeType(new Date())));
            person.addExtension(new Extension(EXT_REJECTED_BY, new StringType(String.valueOf(rejectedByUserId))));
            if (reason != null) {
                person.addExtension(new Extension(EXT_REJECTION_REASON, new StringType(reason)));
            }

            fhirClientService.update(person, getPracticeId());

            log.info("Portal user rejected: {} - Reason: {}", fhirId, reason);

            return ApiResponse.<PortalUserDto>builder()
                    .success(true)
                    .message("Portal user rejected successfully")
                    .data(toPortalUserDto(person))
                    .build();

        } catch (Exception e) {
            log.error("Error rejecting portal user: {}", portalUserId, e);
            return ApiResponse.<PortalUserDto>builder()
                    .success(false)
                    .message("Failed to reject user: " + e.getMessage())
                    .build();
        }
    }

    public ApiResponse<String> getLinkStatus(Long portalUserId) {
        try {
            String fhirId = String.valueOf(portalUserId);
            Person person = fhirClientService.read(Person.class, fhirId, getPracticeId());

            if (person == null) {
                return ApiResponse.<String>builder()
                        .success(true)
                        .message("No patient record found for portal user")
                        .data("NOT_LINKED")
                        .build();
            }

            String ehrPatientId = getStringExt(person, EXT_EHR_PATIENT_ID);
            if (ehrPatientId != null && !ehrPatientId.isEmpty()) {
                return ApiResponse.<String>builder()
                        .success(true)
                        .message("Patient is linked to EHR")
                        .data("LINKED")
                        .build();
            } else {
                return ApiResponse.<String>builder()
                        .success(true)
                        .message("Patient linking is pending approval")
                        .data("PENDING")
                        .build();
            }

        } catch (Exception e) {
            log.error("Error getting link status for portal user: {}", portalUserId, e);
            return ApiResponse.<String>builder()
                    .success(false)
                    .message("Failed to get link status")
                    .build();
        }
    }

    public ApiResponse<String> linkPatient(Long portalUserId, Long ehrPatientId, String requestReason) {
        try {
            log.info("Patient linking request: portalUserId={}, ehrPatientId={}, reason={}",
                    portalUserId, ehrPatientId, requestReason);

            return ApiResponse.<String>builder()
                    .success(true)
                    .message("Patient linking request submitted successfully")
                    .data("REQUEST_SUBMITTED")
                    .build();

        } catch (Exception e) {
            log.error("Error creating patient linking request", e);
            return ApiResponse.<String>builder()
                    .success(false)
                    .message("Failed to create linking request: " + e.getMessage())
                    .build();
        }
    }

    // -------- Helper Methods --------

    private Patient createEhrPatient(Person person) {
        Patient patient = new Patient();

        // Name
        if (person.hasName()) {
            HumanName name = person.getNameFirstRep();
            patient.addName()
                    .setFamily(name.getFamily())
                    .addGiven(name.getGivenAsSingleString());
        }

        // Telecom (email, phone)
        for (ContactPoint cp : person.getTelecom()) {
            patient.addTelecom(cp.copy());
        }

        // Address
        for (Address addr : person.getAddress()) {
            patient.addAddress(addr.copy());
        }

        // Gender
        if (person.hasGender()) {
            patient.setGender(person.getGender());
        }

        // Birth date
        if (person.hasBirthDate()) {
            patient.setBirthDate(person.getBirthDate());
        }

        patient.setActive(true);

        return patient;
    }

    private boolean isPortalUser(Person person) {
        if (!person.hasMeta() || !person.getMeta().hasTag()) return true;
        return person.getMeta().getTag().stream()
                .anyMatch(c -> PORTAL_USER_SYSTEM.equals(c.getSystem()) && PORTAL_USER_CODE.equals(c.getCode()));
    }

    private PortalUserDto toPortalUserDto(Person person) {
        String fhirId = person.getIdElement().getIdPart();

        PortalUserDto dto = PortalUserDto.builder()
                .id((long) Math.abs(fhirId.hashCode()))
                .uuid(fhirId)
                .role("PATIENT")
                .build();

        if (person.hasName()) {
            HumanName name = person.getNameFirstRep();
            dto.setFirstName(name.getGivenAsSingleString());
            dto.setLastName(name.getFamily());
        }

        for (ContactPoint cp : person.getTelecom()) {
            if (cp.getSystem() == ContactPoint.ContactPointSystem.EMAIL) {
                dto.setEmail(cp.getValue());
            } else if (cp.getSystem() == ContactPoint.ContactPointSystem.PHONE) {
                dto.setPhoneNumber(cp.getValue());
            }
        }

        return dto;
    }

    private String getStringExt(Person person, String url) {
        Extension ext = person.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType) {
            return ((StringType) ext.getValue()).getValue();
        }
        return null;
    }
}
