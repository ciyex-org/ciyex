package com.qiaben.ciyex.service.portal;

import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.dto.portal.PortalPatientDto;
import com.qiaben.ciyex.fhir.FhirClientService;
import com.qiaben.ciyex.service.PracticeContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * FHIR-only Portal Patient Service.
 * Uses FHIR Person resource for portal patients.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalPatientService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private static final String EXT_STATUS = "http://ciyex.com/fhir/StructureDefinition/portal-status";
    private static final String EXT_DOB = "http://ciyex.com/fhir/StructureDefinition/date-of-birth";
    private static final String EXT_GENDER = "http://ciyex.com/fhir/StructureDefinition/gender";
    private static final String EXT_ADDRESS1 = "http://ciyex.com/fhir/StructureDefinition/address-line1";
    private static final String EXT_ADDRESS2 = "http://ciyex.com/fhir/StructureDefinition/address-line2";
    private static final String EXT_CITY = "http://ciyex.com/fhir/StructureDefinition/city";
    private static final String EXT_STATE = "http://ciyex.com/fhir/StructureDefinition/state";
    private static final String EXT_POSTAL_CODE = "http://ciyex.com/fhir/StructureDefinition/postal-code";
    private static final String EXT_COUNTRY = "http://ciyex.com/fhir/StructureDefinition/country";
    private static final String EXT_EMERGENCY_NAME = "http://ciyex.com/fhir/StructureDefinition/emergency-contact-name";
    private static final String EXT_EMERGENCY_PHONE = "http://ciyex.com/fhir/StructureDefinition/emergency-contact-phone";
    private static final String EXT_EHR_PATIENT_ID = "http://ciyex.com/fhir/StructureDefinition/ehr-patient-id";
    private static final String EXT_MRN = "http://ciyex.com/fhir/StructureDefinition/medical-record-number";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    public ApiResponse<PortalPatientDto> getPatientInfo(String email) {
        try {
            Bundle bundle = fhirClientService.search(Person.class, getPracticeId());
            Person person = fhirClientService.extractResources(bundle, Person.class).stream()
                    .filter(p -> email.equalsIgnoreCase(getEmail(p)))
                    .findFirst()
                    .orElse(null);

            if (person == null) {
                return ApiResponse.<PortalPatientDto>builder()
                        .success(false)
                        .message("Portal user not found")
                        .build();
            }

            String status = getStringExt(person, EXT_STATUS);
            if (!"APPROVED".equals(status)) {
                return ApiResponse.<PortalPatientDto>builder()
                        .success(false)
                        .message("User not approved")
                        .build();
            }

            PortalPatientDto dto = toPortalPatientDto(person);

            return ApiResponse.<PortalPatientDto>builder()
                    .success(true)
                    .message("Patient information retrieved successfully")
                    .data(dto)
                    .build();

        } catch (Exception e) {
            log.error("Error retrieving patient info for user: {}", email, e);
            return ApiResponse.<PortalPatientDto>builder()
                    .success(false)
                    .message("Failed to retrieve patient information")
                    .build();
        }
    }

    public ApiResponse<PortalPatientDto> updatePatientInfo(String email, PortalPatientDto updateDto) {
        try {
            Bundle bundle = fhirClientService.search(Person.class, getPracticeId());
            Person person = fhirClientService.extractResources(bundle, Person.class).stream()
                    .filter(p -> email.equalsIgnoreCase(getEmail(p)))
                    .findFirst()
                    .orElse(null);

            if (person == null) {
                return ApiResponse.<PortalPatientDto>builder()
                        .success(false)
                        .message("Portal user not found")
                        .build();
            }

            String status = getStringExt(person, EXT_STATUS);
            if (!"APPROVED".equals(status)) {
                return ApiResponse.<PortalPatientDto>builder()
                        .success(false)
                        .message("User not approved")
                        .build();
            }

            // Update fields
            if (updateDto.getAddressLine1() != null) {
                person.getExtension().removeIf(e -> EXT_ADDRESS1.equals(e.getUrl()));
                person.addExtension(new Extension(EXT_ADDRESS1, new StringType(updateDto.getAddressLine1())));
            }
            if (updateDto.getAddressLine2() != null) {
                person.getExtension().removeIf(e -> EXT_ADDRESS2.equals(e.getUrl()));
                person.addExtension(new Extension(EXT_ADDRESS2, new StringType(updateDto.getAddressLine2())));
            }
            if (updateDto.getCity() != null) {
                person.getExtension().removeIf(e -> EXT_CITY.equals(e.getUrl()));
                person.addExtension(new Extension(EXT_CITY, new StringType(updateDto.getCity())));
            }
            if (updateDto.getState() != null) {
                person.getExtension().removeIf(e -> EXT_STATE.equals(e.getUrl()));
                person.addExtension(new Extension(EXT_STATE, new StringType(updateDto.getState())));
            }
            if (updateDto.getPostalCode() != null) {
                person.getExtension().removeIf(e -> EXT_POSTAL_CODE.equals(e.getUrl()));
                person.addExtension(new Extension(EXT_POSTAL_CODE, new StringType(updateDto.getPostalCode())));
            }
            if (updateDto.getCountry() != null) {
                person.getExtension().removeIf(e -> EXT_COUNTRY.equals(e.getUrl()));
                person.addExtension(new Extension(EXT_COUNTRY, new StringType(updateDto.getCountry())));
            }
            if (updateDto.getEmergencyContactName() != null) {
                person.getExtension().removeIf(e -> EXT_EMERGENCY_NAME.equals(e.getUrl()));
                person.addExtension(new Extension(EXT_EMERGENCY_NAME, new StringType(updateDto.getEmergencyContactName())));
            }
            if (updateDto.getEmergencyContactPhone() != null) {
                person.getExtension().removeIf(e -> EXT_EMERGENCY_PHONE.equals(e.getUrl()));
                person.addExtension(new Extension(EXT_EMERGENCY_PHONE, new StringType(updateDto.getEmergencyContactPhone())));
            }

            fhirClientService.update(person, getPracticeId());

            PortalPatientDto dto = toPortalPatientDto(person);

            return ApiResponse.<PortalPatientDto>builder()
                    .success(true)
                    .message("Patient information updated successfully")
                    .data(dto)
                    .build();

        } catch (Exception e) {
            log.error("Error updating patient info for user: {}", email, e);
            return ApiResponse.<PortalPatientDto>builder()
                    .success(false)
                    .message("Failed to update patient information")
                    .build();
        }
    }

    // -------- Helper Methods --------

    private PortalPatientDto toPortalPatientDto(Person person) {
        String fhirId = person.getIdElement().getIdPart();

        PortalPatientDto dto = PortalPatientDto.builder()
                .id((long) Math.abs(fhirId.hashCode()))
                .portalUserId((long) Math.abs(fhirId.hashCode()))
                .addressLine1(getStringExt(person, EXT_ADDRESS1))
                .addressLine2(getStringExt(person, EXT_ADDRESS2))
                .city(getStringExt(person, EXT_CITY))
                .state(getStringExt(person, EXT_STATE))
                .postalCode(getStringExt(person, EXT_POSTAL_CODE))
                .country(getStringExt(person, EXT_COUNTRY))
                .emergencyContactName(getStringExt(person, EXT_EMERGENCY_NAME))
                .emergencyContactPhone(getStringExt(person, EXT_EMERGENCY_PHONE))
                .gender(getStringExt(person, EXT_GENDER))
                .medicalRecordNumber(getStringExt(person, EXT_MRN))
                .build();

        // Name
        if (person.hasName()) {
            HumanName name = person.getNameFirstRep();
            dto.setFirstName(name.getGivenAsSingleString());
            dto.setLastName(name.getFamily());
        }

        // Email & Phone
        dto.setEmail(getEmail(person));
        dto.setPhoneNumber(getPhone(person));

        // DOB
        Extension dobExt = person.getExtensionByUrl(EXT_DOB);
        if (dobExt != null && dobExt.getValue() instanceof DateType) {
            Date date = ((DateType) dobExt.getValue()).getValue();
            if (date != null) {
                dto.setDateOfBirth(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            }
        }

        // EHR Patient ID
        String ehrPatientIdStr = getStringExt(person, EXT_EHR_PATIENT_ID);
        if (ehrPatientIdStr != null) {
            try {
                dto.setEhrPatientId(Long.parseLong(ehrPatientIdStr));
            } catch (NumberFormatException ignored) {}
        }

        return dto;
    }

    private String getEmail(Person person) {
        return person.getTelecom().stream()
                .filter(cp -> cp.getSystem() == ContactPoint.ContactPointSystem.EMAIL)
                .map(ContactPoint::getValue)
                .findFirst()
                .orElse(null);
    }

    private String getPhone(Person person) {
        return person.getTelecom().stream()
                .filter(cp -> cp.getSystem() == ContactPoint.ContactPointSystem.PHONE)
                .map(ContactPoint::getValue)
                .findFirst()
                .orElse(null);
    }

    private String getStringExt(Person person, String url) {
        Extension ext = person.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType) {
            return ((StringType) ext.getValue()).getValue();
        }
        return null;
    }
}
