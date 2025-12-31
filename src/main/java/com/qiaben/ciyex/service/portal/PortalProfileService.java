package com.qiaben.ciyex.service.portal;

import com.qiaben.ciyex.dto.portal.PortalProfileDto;
import com.qiaben.ciyex.fhir.FhirClientService;
import com.qiaben.ciyex.service.PracticeContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * FHIR-only Portal Profile Service.
 * Uses FHIR Basic resource for storing portal user profiles.
 * No local database storage - all data stored in FHIR server.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalProfileService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private static final String PROFILE_TYPE_SYSTEM = "http://ciyex.com/fhir/resource-type";
    private static final String PROFILE_TYPE_CODE = "portal-profile";
    private static final String EXT_USER_ID = "http://ciyex.com/fhir/StructureDefinition/user-id";
    private static final String EXT_FIRST_NAME = "http://ciyex.com/fhir/StructureDefinition/first-name";
    private static final String EXT_LAST_NAME = "http://ciyex.com/fhir/StructureDefinition/last-name";
    private static final String EXT_PHONE = "http://ciyex.com/fhir/StructureDefinition/phone";
    private static final String EXT_EMAIL = "http://ciyex.com/fhir/StructureDefinition/email";
    private static final String EXT_DOB = "http://ciyex.com/fhir/StructureDefinition/date-of-birth";
    private static final String EXT_STREET = "http://ciyex.com/fhir/StructureDefinition/street";
    private static final String EXT_CITY = "http://ciyex.com/fhir/StructureDefinition/city";
    private static final String EXT_STATE = "http://ciyex.com/fhir/StructureDefinition/state";
    private static final String EXT_POSTAL_CODE = "http://ciyex.com/fhir/StructureDefinition/postal-code";
    private static final String EXT_COUNTRY = "http://ciyex.com/fhir/StructureDefinition/country";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    public PortalProfileDto getProfile(UUID userId) {
        log.debug("Getting portal profile for user: {}", userId);

        Bundle bundle = fhirClientService.search(Basic.class, getPracticeId());
        List<Basic> profiles = fhirClientService.extractResources(bundle, Basic.class).stream()
                .filter(this::isPortalProfile)
                .filter(b -> userId.toString().equals(getStringExt(b, EXT_USER_ID)))
                .toList();

        if (profiles.isEmpty()) {
            return null;
        }

        return fromFhirBasic(profiles.get(0));
    }

    public PortalProfileDto updateProfile(UUID userId, PortalProfileDto dto) {
        log.debug("Updating portal profile for user: {}", userId);

        Bundle bundle = fhirClientService.search(Basic.class, getPracticeId());
        List<Basic> profiles = fhirClientService.extractResources(bundle, Basic.class).stream()
                .filter(this::isPortalProfile)
                .filter(b -> userId.toString().equals(getStringExt(b, EXT_USER_ID)))
                .toList();

        dto.setUserId(userId);
        Basic basic = toFhirBasic(dto);

        if (profiles.isEmpty()) {
            // Create new
            var outcome = fhirClientService.create(basic, getPracticeId());
            String fhirId = outcome.getId().getIdPart();
            dto.setId((long) Math.abs(fhirId.hashCode()));
            log.info("Created portal profile with FHIR ID: {}", fhirId);
        } else {
            // Update existing
            String fhirId = profiles.get(0).getIdElement().getIdPart();
            basic.setId(fhirId);
            fhirClientService.update(basic, getPracticeId());
            dto.setId((long) Math.abs(fhirId.hashCode()));
            log.info("Updated portal profile with FHIR ID: {}", fhirId);
        }

        return dto;
    }

    // -------- FHIR Mapping --------

    private Basic toFhirBasic(PortalProfileDto dto) {
        Basic basic = new Basic();

        CodeableConcept code = new CodeableConcept();
        code.addCoding().setSystem(PROFILE_TYPE_SYSTEM).setCode(PROFILE_TYPE_CODE).setDisplay("Portal Profile");
        basic.setCode(code);

        if (dto.getUserId() != null) {
            basic.addExtension(new Extension(EXT_USER_ID, new StringType(dto.getUserId().toString())));
        }
        if (dto.getFirstName() != null) {
            basic.addExtension(new Extension(EXT_FIRST_NAME, new StringType(dto.getFirstName())));
        }
        if (dto.getLastName() != null) {
            basic.addExtension(new Extension(EXT_LAST_NAME, new StringType(dto.getLastName())));
        }
        if (dto.getPhone() != null) {
            basic.addExtension(new Extension(EXT_PHONE, new StringType(dto.getPhone())));
        }
        if (dto.getEmail() != null) {
            basic.addExtension(new Extension(EXT_EMAIL, new StringType(dto.getEmail())));
        }
        if (dto.getDateOfBirth() != null) {
            basic.addExtension(new Extension(EXT_DOB, new DateType(Date.from(dto.getDateOfBirth().atStartOfDay(ZoneId.systemDefault()).toInstant()))));
        }
        if (dto.getStreet() != null) {
            basic.addExtension(new Extension(EXT_STREET, new StringType(dto.getStreet())));
        }
        if (dto.getCity() != null) {
            basic.addExtension(new Extension(EXT_CITY, new StringType(dto.getCity())));
        }
        if (dto.getState() != null) {
            basic.addExtension(new Extension(EXT_STATE, new StringType(dto.getState())));
        }
        if (dto.getPostalCode() != null) {
            basic.addExtension(new Extension(EXT_POSTAL_CODE, new StringType(dto.getPostalCode())));
        }
        if (dto.getCountry() != null) {
            basic.addExtension(new Extension(EXT_COUNTRY, new StringType(dto.getCountry())));
        }

        return basic;
    }

    private PortalProfileDto fromFhirBasic(Basic basic) {
        String fhirId = basic.getIdElement().getIdPart();

        PortalProfileDto.PortalProfileDtoBuilder builder = PortalProfileDto.builder()
                .id((long) Math.abs(fhirId.hashCode()))
                .firstName(getStringExt(basic, EXT_FIRST_NAME))
                .lastName(getStringExt(basic, EXT_LAST_NAME))
                .phone(getStringExt(basic, EXT_PHONE))
                .email(getStringExt(basic, EXT_EMAIL))
                .street(getStringExt(basic, EXT_STREET))
                .city(getStringExt(basic, EXT_CITY))
                .state(getStringExt(basic, EXT_STATE))
                .postalCode(getStringExt(basic, EXT_POSTAL_CODE))
                .country(getStringExt(basic, EXT_COUNTRY));

        String userIdStr = getStringExt(basic, EXT_USER_ID);
        if (userIdStr != null) {
            try {
                builder.userId(UUID.fromString(userIdStr));
            } catch (IllegalArgumentException ignored) {}
        }

        Extension dobExt = basic.getExtensionByUrl(EXT_DOB);
        if (dobExt != null && dobExt.getValue() instanceof DateType) {
            Date date = ((DateType) dobExt.getValue()).getValue();
            if (date != null) {
                builder.dateOfBirth(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            }
        }

        return builder.build();
    }

    private boolean isPortalProfile(Basic basic) {
        if (!basic.hasCode()) return false;
        return basic.getCode().getCoding().stream()
                .anyMatch(c -> PROFILE_TYPE_SYSTEM.equals(c.getSystem()) && PROFILE_TYPE_CODE.equals(c.getCode()));
    }

    private String getStringExt(Basic basic, String url) {
        Extension ext = basic.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType) {
            return ((StringType) ext.getValue()).getValue();
        }
        return null;
    }
}