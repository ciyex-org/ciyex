package com.qiaben.ciyex.service.portal;

import com.qiaben.ciyex.dto.portal.PortalDemographicsDto;
import com.qiaben.ciyex.fhir.FhirClientService;
import com.qiaben.ciyex.service.PracticeContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * FHIR-only Portal Demographics Service.
 * Uses FHIR Basic resource for storing portal demographics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortalDemographicsService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private static final String DEMO_TYPE_SYSTEM = "http://ciyex.com/fhir/resource-type";
    private static final String DEMO_TYPE_CODE = "portal-demographics";
    private static final String EXT_USER_ID = "http://ciyex.com/fhir/StructureDefinition/user-id";
    private static final String EXT_FIRST_NAME = "http://ciyex.com/fhir/StructureDefinition/first-name";
    private static final String EXT_MIDDLE_NAME = "http://ciyex.com/fhir/StructureDefinition/middle-name";
    private static final String EXT_LAST_NAME = "http://ciyex.com/fhir/StructureDefinition/last-name";
    private static final String EXT_DOB = "http://ciyex.com/fhir/StructureDefinition/dob";
    private static final String EXT_SEX = "http://ciyex.com/fhir/StructureDefinition/sex";
    private static final String EXT_MARITAL_STATUS = "http://ciyex.com/fhir/StructureDefinition/marital-status";
    private static final String EXT_ADDRESS = "http://ciyex.com/fhir/StructureDefinition/address";
    private static final String EXT_CITY = "http://ciyex.com/fhir/StructureDefinition/city";
    private static final String EXT_STATE = "http://ciyex.com/fhir/StructureDefinition/state";
    private static final String EXT_POSTAL_CODE = "http://ciyex.com/fhir/StructureDefinition/postal-code";
    private static final String EXT_COUNTRY = "http://ciyex.com/fhir/StructureDefinition/country";
    private static final String EXT_PHONE_MOBILE = "http://ciyex.com/fhir/StructureDefinition/phone-mobile";
    private static final String EXT_CONTACT_EMAIL = "http://ciyex.com/fhir/StructureDefinition/contact-email";
    private static final String EXT_EMERGENCY_NAME = "http://ciyex.com/fhir/StructureDefinition/emergency-contact-name";
    private static final String EXT_EMERGENCY_PHONE = "http://ciyex.com/fhir/StructureDefinition/emergency-contact-phone";
    private static final String EXT_ALLOW_SMS = "http://ciyex.com/fhir/StructureDefinition/allow-sms";
    private static final String EXT_ALLOW_EMAIL = "http://ciyex.com/fhir/StructureDefinition/allow-email";
    private static final String EXT_ALLOW_VOICE = "http://ciyex.com/fhir/StructureDefinition/allow-voice-message";
    private static final String EXT_ALLOW_MAIL = "http://ciyex.com/fhir/StructureDefinition/allow-mail-message";
    private static final String EXT_CREATED_DATE = "http://ciyex.com/fhir/StructureDefinition/created-date";
    private static final String EXT_MODIFIED_DATE = "http://ciyex.com/fhir/StructureDefinition/last-modified-date";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    public PortalDemographicsDto getMyDemographics(UUID userId) {
        log.debug("Getting demographics for user: {}", userId);

        Bundle bundle = fhirClientService.search(Basic.class, getPracticeId());
        List<Basic> demos = fhirClientService.extractResources(bundle, Basic.class).stream()
                .filter(this::isPortalDemographics)
                .filter(b -> userId.toString().equals(getStringExt(b, EXT_USER_ID)))
                .toList();

        if (demos.isEmpty()) {
            // Return blank demographics
            PortalDemographicsDto blank = new PortalDemographicsDto();
            return blank;
        }

        return fromFhirBasic(demos.get(0));
    }

    public PortalDemographicsDto updateMyDemographics(UUID userId, PortalDemographicsDto dto) {
        log.debug("Updating demographics for user: {}", userId);

        Bundle bundle = fhirClientService.search(Basic.class, getPracticeId());
        List<Basic> demos = fhirClientService.extractResources(bundle, Basic.class).stream()
                .filter(this::isPortalDemographics)
                .filter(b -> userId.toString().equals(getStringExt(b, EXT_USER_ID)))
                .toList();

        Instant now = Instant.now();
        PortalDemographicsDto.Audit audit = dto.getAudit() != null ? dto.getAudit() : new PortalDemographicsDto.Audit();

        if (demos.isEmpty()) {
            // Create new
            audit.setCreatedDate(now);
            audit.setLastModifiedDate(now);
            dto.setAudit(audit);

            Basic basic = toFhirBasic(dto, userId);
            var outcome = fhirClientService.create(basic, getPracticeId());
            String fhirId = outcome.getId().getIdPart();
            dto.setId((long) Math.abs(fhirId.hashCode()));
            dto.setFhirId(fhirId);
            log.info("Created portal demographics with FHIR ID: {}", fhirId);
        } else {
            // Update existing - preserve createdDate
            Basic existing = demos.get(0);
            Extension createdExt = existing.getExtensionByUrl(EXT_CREATED_DATE);
            if (createdExt != null && createdExt.getValue() instanceof InstantType) {
                audit.setCreatedDate(((InstantType) createdExt.getValue()).getValueAsCalendar().toInstant());
            }
            audit.setLastModifiedDate(now);
            dto.setAudit(audit);

            String fhirId = existing.getIdElement().getIdPart();
            Basic basic = toFhirBasic(dto, userId);
            basic.setId(fhirId);
            fhirClientService.update(basic, getPracticeId());
            dto.setId((long) Math.abs(fhirId.hashCode()));
            dto.setFhirId(fhirId);
            log.info("Updated portal demographics with FHIR ID: {}", fhirId);
        }

        return dto;
    }

    // -------- FHIR Mapping --------

    private Basic toFhirBasic(PortalDemographicsDto dto, UUID userId) {
        Basic basic = new Basic();

        CodeableConcept code = new CodeableConcept();
        code.addCoding().setSystem(DEMO_TYPE_SYSTEM).setCode(DEMO_TYPE_CODE).setDisplay("Portal Demographics");
        basic.setCode(code);

        basic.addExtension(new Extension(EXT_USER_ID, new StringType(userId.toString())));

        if (dto.getFirstName() != null) basic.addExtension(new Extension(EXT_FIRST_NAME, new StringType(dto.getFirstName())));
        if (dto.getMiddleName() != null) basic.addExtension(new Extension(EXT_MIDDLE_NAME, new StringType(dto.getMiddleName())));
        if (dto.getLastName() != null) basic.addExtension(new Extension(EXT_LAST_NAME, new StringType(dto.getLastName())));
        if (dto.getDob() != null) basic.addExtension(new Extension(EXT_DOB, new DateType(Date.from(dto.getDob().atStartOfDay(ZoneId.systemDefault()).toInstant()))));
        if (dto.getSex() != null) basic.addExtension(new Extension(EXT_SEX, new StringType(dto.getSex())));
        if (dto.getMaritalStatus() != null) basic.addExtension(new Extension(EXT_MARITAL_STATUS, new StringType(dto.getMaritalStatus())));
        if (dto.getAddress() != null) basic.addExtension(new Extension(EXT_ADDRESS, new StringType(dto.getAddress())));
        if (dto.getCity() != null) basic.addExtension(new Extension(EXT_CITY, new StringType(dto.getCity())));
        if (dto.getState() != null) basic.addExtension(new Extension(EXT_STATE, new StringType(dto.getState())));
        if (dto.getPostalCode() != null) basic.addExtension(new Extension(EXT_POSTAL_CODE, new StringType(dto.getPostalCode())));
        if (dto.getCountry() != null) basic.addExtension(new Extension(EXT_COUNTRY, new StringType(dto.getCountry())));
        if (dto.getPhoneMobile() != null) basic.addExtension(new Extension(EXT_PHONE_MOBILE, new StringType(dto.getPhoneMobile())));
        if (dto.getContactEmail() != null) basic.addExtension(new Extension(EXT_CONTACT_EMAIL, new StringType(dto.getContactEmail())));
        if (dto.getEmergencyContactName() != null) basic.addExtension(new Extension(EXT_EMERGENCY_NAME, new StringType(dto.getEmergencyContactName())));
        if (dto.getEmergencyContactPhone() != null) basic.addExtension(new Extension(EXT_EMERGENCY_PHONE, new StringType(dto.getEmergencyContactPhone())));
        basic.addExtension(new Extension(EXT_ALLOW_SMS, new BooleanType(dto.isAllowSMS())));
        basic.addExtension(new Extension(EXT_ALLOW_EMAIL, new BooleanType(dto.isAllowEmail())));
        basic.addExtension(new Extension(EXT_ALLOW_VOICE, new BooleanType(dto.isAllowVoiceMessage())));
        basic.addExtension(new Extension(EXT_ALLOW_MAIL, new BooleanType(dto.isAllowMailMessage())));

        if (dto.getAudit() != null) {
            if (dto.getAudit().getCreatedDate() != null) {
                basic.addExtension(new Extension(EXT_CREATED_DATE, new InstantType(Date.from(dto.getAudit().getCreatedDate()))));
            }
            if (dto.getAudit().getLastModifiedDate() != null) {
                basic.addExtension(new Extension(EXT_MODIFIED_DATE, new InstantType(Date.from(dto.getAudit().getLastModifiedDate()))));
            }
        }

        return basic;
    }

    private PortalDemographicsDto fromFhirBasic(Basic basic) {
        PortalDemographicsDto dto = new PortalDemographicsDto();

        String fhirId = basic.getIdElement().getIdPart();
        dto.setId((long) Math.abs(fhirId.hashCode()));
        dto.setFhirId(fhirId);

        dto.setFirstName(getStringExt(basic, EXT_FIRST_NAME));
        dto.setMiddleName(getStringExt(basic, EXT_MIDDLE_NAME));
        dto.setLastName(getStringExt(basic, EXT_LAST_NAME));
        dto.setSex(getStringExt(basic, EXT_SEX));
        dto.setMaritalStatus(getStringExt(basic, EXT_MARITAL_STATUS));
        dto.setAddress(getStringExt(basic, EXT_ADDRESS));
        dto.setCity(getStringExt(basic, EXT_CITY));
        dto.setState(getStringExt(basic, EXT_STATE));
        dto.setPostalCode(getStringExt(basic, EXT_POSTAL_CODE));
        dto.setCountry(getStringExt(basic, EXT_COUNTRY));
        dto.setPhoneMobile(getStringExt(basic, EXT_PHONE_MOBILE));
        dto.setContactEmail(getStringExt(basic, EXT_CONTACT_EMAIL));
        dto.setEmergencyContactName(getStringExt(basic, EXT_EMERGENCY_NAME));
        dto.setEmergencyContactPhone(getStringExt(basic, EXT_EMERGENCY_PHONE));
        dto.setAllowSMS(getBoolExt(basic, EXT_ALLOW_SMS));
        dto.setAllowEmail(getBoolExt(basic, EXT_ALLOW_EMAIL));
        dto.setAllowVoiceMessage(getBoolExt(basic, EXT_ALLOW_VOICE));
        dto.setAllowMailMessage(getBoolExt(basic, EXT_ALLOW_MAIL));

        Extension dobExt = basic.getExtensionByUrl(EXT_DOB);
        if (dobExt != null && dobExt.getValue() instanceof DateType) {
            Date date = ((DateType) dobExt.getValue()).getValue();
            if (date != null) {
                dto.setDob(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            }
        }

        PortalDemographicsDto.Audit audit = new PortalDemographicsDto.Audit();
        Extension createdExt = basic.getExtensionByUrl(EXT_CREATED_DATE);
        if (createdExt != null && createdExt.getValue() instanceof InstantType) {
            audit.setCreatedDate(((InstantType) createdExt.getValue()).getValueAsCalendar().toInstant());
        }
        Extension modifiedExt = basic.getExtensionByUrl(EXT_MODIFIED_DATE);
        if (modifiedExt != null && modifiedExt.getValue() instanceof InstantType) {
            audit.setLastModifiedDate(((InstantType) modifiedExt.getValue()).getValueAsCalendar().toInstant());
        }
        dto.setAudit(audit);

        return dto;
    }

    private boolean isPortalDemographics(Basic basic) {
        if (!basic.hasCode()) return false;
        return basic.getCode().getCoding().stream()
                .anyMatch(c -> DEMO_TYPE_SYSTEM.equals(c.getSystem()) && DEMO_TYPE_CODE.equals(c.getCode()));
    }

    private String getStringExt(Basic basic, String url) {
        Extension ext = basic.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType) {
            return ((StringType) ext.getValue()).getValue();
        }
        return null;
    }

    private boolean getBoolExt(Basic basic, String url) {
        Extension ext = basic.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof BooleanType) {
            return ((BooleanType) ext.getValue()).booleanValue();
        }
        return false;
    }
}
