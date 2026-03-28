package org.ciyex.ehr.service;

import org.ciyex.ehr.dto.CreditCardDto;
import org.ciyex.ehr.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * FHIR-only Credit Card Service.
 * Uses FHIR Basic resource for storing credit card data.
 * No local database storage - all data stored in FHIR server.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CreditCardService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private static final String CARD_TYPE_SYSTEM = "http://ciyex.com/fhir/resource-type";
    private static final String CARD_TYPE_CODE = "credit-card";
    private static final String EXT_PATIENT_ID = "http://ciyex.com/fhir/StructureDefinition/patient-id";
    private static final String EXT_CARD_HOLDER = "http://ciyex.com/fhir/StructureDefinition/card-holder-name";
    private static final String EXT_CARD_NUMBER = "http://ciyex.com/fhir/StructureDefinition/card-number";
    private static final String EXT_CARD_TYPE = "http://ciyex.com/fhir/StructureDefinition/card-type";
    private static final String EXT_EXPIRY_MONTH = "http://ciyex.com/fhir/StructureDefinition/expiry-month";
    private static final String EXT_EXPIRY_YEAR = "http://ciyex.com/fhir/StructureDefinition/expiry-year";
    private static final String EXT_CVV = "http://ciyex.com/fhir/StructureDefinition/cvv";
    private static final String EXT_BILLING_ADDRESS = "http://ciyex.com/fhir/StructureDefinition/billing-address";
    private static final String EXT_BILLING_CITY = "http://ciyex.com/fhir/StructureDefinition/billing-city";
    private static final String EXT_BILLING_STATE = "http://ciyex.com/fhir/StructureDefinition/billing-state";
    private static final String EXT_BILLING_ZIP = "http://ciyex.com/fhir/StructureDefinition/billing-zip";
    private static final String EXT_BILLING_COUNTRY = "http://ciyex.com/fhir/StructureDefinition/billing-country";
    private static final String EXT_IS_DEFAULT = "http://ciyex.com/fhir/StructureDefinition/is-default";
    private static final String EXT_IS_ACTIVE = "http://ciyex.com/fhir/StructureDefinition/is-active";
    private static final String EXT_TOKEN = "http://ciyex.com/fhir/StructureDefinition/token";

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // CREATE
    public CreditCardDto create(CreditCardDto dto) {
        log.info("Creating credit card for patient ID: {}", dto.getPatientId());

        validateCardExpiration(dto.getExpiryMonth(), dto.getExpiryYear());

        // If this is marked as default, unset other default cards
        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            unsetDefaultCards(dto.getPatientId());
        }

        // Detect card type if not provided
        if (dto.getCardType() == null || dto.getCardType().isBlank()) {
            dto.setCardType(detectCardType(dto.getCardNumber()));
        }

        Basic basic = toFhirBasic(dto);
        var outcome = fhirClientService.create(basic, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setId((long) Math.abs(fhirId.hashCode()));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        dto.setMaskedCardNumber(maskCardNumber(dto.getCardNumber()));
        dto.setIsExpired(isExpired(dto.getExpiryMonth(), dto.getExpiryYear()));

        log.info("Credit card created successfully with FHIR ID: {}", fhirId);
        return dto;
    }

    // GET BY ID
    public CreditCardDto getById(String fhirId) {
        log.info("Retrieving credit card with FHIR ID: {}", fhirId);
        Basic basic = fhirClientService.read(Basic.class, fhirId, getPracticeId());
        return fromFhirBasic(basic);
    }

    // GET ALL BY PATIENT
    public List<CreditCardDto> getByPatientId(Long patientId) {
        log.info("Retrieving all credit cards for patient ID: {}", patientId);
        Bundle bundle = fhirClientService.search(Basic.class, getPracticeId());

        return fhirClientService.extractResources(bundle, Basic.class).stream()
                .filter(this::isCreditCard)
                .map(this::fromFhirBasic)
                .filter(dto -> patientId.equals(dto.getPatientId()))
                .collect(Collectors.toList());
    }

    // GET ACTIVE CARDS BY PATIENT
    public List<CreditCardDto> getActiveCardsByPatientId(Long patientId) {
        log.info("Retrieving active credit cards for patient ID: {}", patientId);
        return getByPatientId(patientId).stream()
                .filter(dto -> Boolean.TRUE.equals(dto.getIsActive()))
                .collect(Collectors.toList());
    }

    // GET DEFAULT CARD
    public CreditCardDto getDefaultCard(Long patientId) {
        log.info("Retrieving default credit card for patient ID: {}", patientId);
        return getByPatientId(patientId).stream()
                .filter(dto -> Boolean.TRUE.equals(dto.getIsDefault()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No default card found for patient ID: " + patientId));
    }

    // UPDATE
    public CreditCardDto update(String fhirId, CreditCardDto dto) {
        log.info("Updating credit card with FHIR ID: {}", fhirId);

        validateCardExpiration(dto.getExpiryMonth(), dto.getExpiryYear());

        // If this is marked as default, unset other default cards
        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            unsetDefaultCards(dto.getPatientId());
        }

        Basic basic = toFhirBasic(dto);
        basic.setId(fhirId);
        fhirClientService.update(basic, getPracticeId());

        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        dto.setMaskedCardNumber(maskCardNumber(dto.getCardNumber()));
        dto.setIsExpired(isExpired(dto.getExpiryMonth(), dto.getExpiryYear()));

        log.info("Credit card updated successfully with FHIR ID: {}", fhirId);
        return dto;
    }

    // SET AS DEFAULT
    public CreditCardDto setAsDefault(String fhirId, Long patientId) {
        log.info("Setting credit card {} as default for patient {}", fhirId, patientId);

        unsetDefaultCards(patientId);

        Basic basic = fhirClientService.read(Basic.class, fhirId, getPracticeId());
        basic.getExtension().removeIf(e -> EXT_IS_DEFAULT.equals(e.getUrl()));
        basic.addExtension(new Extension(EXT_IS_DEFAULT, new BooleanType(true)));
        fhirClientService.update(basic, getPracticeId());

        log.info("Credit card set as default successfully");
        return fromFhirBasic(basic);
    }

    // DEACTIVATE
    public void deactivate(String fhirId) {
        log.info("Deactivating credit card with FHIR ID: {}", fhirId);

        Basic basic = fhirClientService.read(Basic.class, fhirId, getPracticeId());
        basic.getExtension().removeIf(e -> EXT_IS_ACTIVE.equals(e.getUrl()) || EXT_IS_DEFAULT.equals(e.getUrl()));
        basic.addExtension(new Extension(EXT_IS_ACTIVE, new BooleanType(false)));
        basic.addExtension(new Extension(EXT_IS_DEFAULT, new BooleanType(false)));
        fhirClientService.update(basic, getPracticeId());

        log.info("Credit card deactivated successfully");
    }

    // DELETE
    public void delete(String fhirId) {
        log.info("Deleting credit card with FHIR ID: {}", fhirId);
        fhirClientService.delete(Basic.class, fhirId, getPracticeId());
        log.info("Credit card deleted successfully");
    }

    // -------- FHIR Mapping --------

    private Basic toFhirBasic(CreditCardDto dto) {
        Basic basic = new Basic();

        CodeableConcept code = new CodeableConcept();
        code.addCoding().setSystem(CARD_TYPE_SYSTEM).setCode(CARD_TYPE_CODE).setDisplay("Credit Card");
        basic.setCode(code);

        if (dto.getPatientId() != null) {
            basic.addExtension(new Extension(EXT_PATIENT_ID, new StringType(dto.getPatientId().toString())));
        }
        if (dto.getCardHolderName() != null) {
            basic.addExtension(new Extension(EXT_CARD_HOLDER, new StringType(dto.getCardHolderName())));
        }
        if (dto.getCardNumber() != null) {
            basic.addExtension(new Extension(EXT_CARD_NUMBER, new StringType(dto.getCardNumber())));
        }
        if (dto.getCardType() != null) {
            basic.addExtension(new Extension(EXT_CARD_TYPE, new StringType(dto.getCardType())));
        }
        if (dto.getExpiryMonth() != null) {
            basic.addExtension(new Extension(EXT_EXPIRY_MONTH, new IntegerType(dto.getExpiryMonth())));
        }
        if (dto.getExpiryYear() != null) {
            basic.addExtension(new Extension(EXT_EXPIRY_YEAR, new IntegerType(dto.getExpiryYear())));
        }
        if (dto.getCvv() != null) {
            basic.addExtension(new Extension(EXT_CVV, new StringType(dto.getCvv())));
        }
        if (dto.getBillingAddress() != null) {
            basic.addExtension(new Extension(EXT_BILLING_ADDRESS, new StringType(dto.getBillingAddress())));
        }
        if (dto.getBillingCity() != null) {
            basic.addExtension(new Extension(EXT_BILLING_CITY, new StringType(dto.getBillingCity())));
        }
        if (dto.getBillingState() != null) {
            basic.addExtension(new Extension(EXT_BILLING_STATE, new StringType(dto.getBillingState())));
        }
        if (dto.getBillingZip() != null) {
            basic.addExtension(new Extension(EXT_BILLING_ZIP, new StringType(dto.getBillingZip())));
        }
        if (dto.getBillingCountry() != null) {
            basic.addExtension(new Extension(EXT_BILLING_COUNTRY, new StringType(dto.getBillingCountry())));
        }
        basic.addExtension(new Extension(EXT_IS_DEFAULT, new BooleanType(Boolean.TRUE.equals(dto.getIsDefault()))));
        basic.addExtension(new Extension(EXT_IS_ACTIVE, new BooleanType(dto.getIsActive() == null || dto.getIsActive())));
        if (dto.getToken() != null) {
            basic.addExtension(new Extension(EXT_TOKEN, new StringType(dto.getToken())));
        }

        return basic;
    }

    private CreditCardDto fromFhirBasic(Basic basic) {
        String fhirId = basic.getIdElement().getIdPart();

        CreditCardDto dto = CreditCardDto.builder()
                .id((long) Math.abs(fhirId.hashCode()))
                .fhirId(fhirId)
                .externalId(fhirId)
                .patientId(getLongExt(basic, EXT_PATIENT_ID))
                .cardHolderName(getStringExt(basic, EXT_CARD_HOLDER))
                .cardNumber(getStringExt(basic, EXT_CARD_NUMBER))
                .cardType(getStringExt(basic, EXT_CARD_TYPE))
                .expiryMonth(getIntExt(basic, EXT_EXPIRY_MONTH))
                .expiryYear(getIntExt(basic, EXT_EXPIRY_YEAR))
                .cvv(getStringExt(basic, EXT_CVV))
                .billingAddress(getStringExt(basic, EXT_BILLING_ADDRESS))
                .billingCity(getStringExt(basic, EXT_BILLING_CITY))
                .billingState(getStringExt(basic, EXT_BILLING_STATE))
                .billingZip(getStringExt(basic, EXT_BILLING_ZIP))
                .billingCountry(getStringExt(basic, EXT_BILLING_COUNTRY))
                .isDefault(getBoolExt(basic, EXT_IS_DEFAULT))
                .isActive(getBoolExt(basic, EXT_IS_ACTIVE))
                .token(getStringExt(basic, EXT_TOKEN))
                .build();

        dto.setMaskedCardNumber(maskCardNumber(dto.getCardNumber()));
        dto.setIsExpired(isExpired(dto.getExpiryMonth(), dto.getExpiryYear()));

        CreditCardDto.Audit audit = new CreditCardDto.Audit();
        audit.setCreatedDate(LocalDate.now().format(DAY));
        audit.setLastModifiedDate(LocalDate.now().format(DAY));
        dto.setAudit(audit);

        return dto;
    }

    private boolean isCreditCard(Basic basic) {
        if (!basic.hasCode()) return false;
        return basic.getCode().getCoding().stream()
                .anyMatch(c -> CARD_TYPE_SYSTEM.equals(c.getSystem()) && CARD_TYPE_CODE.equals(c.getCode()));
    }

    // -------- Helpers --------

    private void validateCardExpiration(Integer month, Integer year) {
        if (month == null || year == null) return;
        LocalDate now = LocalDate.now();
        LocalDate expiry = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1);
        if (now.isAfter(expiry)) {
            throw new IllegalArgumentException("Credit card is expired");
        }
    }

    private boolean isExpired(Integer month, Integer year) {
        if (month == null || year == null) return false;
        LocalDate now = LocalDate.now();
        LocalDate expiry = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1);
        return now.isAfter(expiry);
    }

    private void unsetDefaultCards(Long patientId) {
        Bundle bundle = fhirClientService.search(Basic.class, getPracticeId());
        fhirClientService.extractResources(bundle, Basic.class).stream()
                .filter(this::isCreditCard)
                .filter(basic -> patientId.equals(getLongExt(basic, EXT_PATIENT_ID)))
                .filter(basic -> Boolean.TRUE.equals(getBoolExt(basic, EXT_IS_DEFAULT)))
                .forEach(basic -> {
                    basic.getExtension().removeIf(e -> EXT_IS_DEFAULT.equals(e.getUrl()));
                    basic.addExtension(new Extension(EXT_IS_DEFAULT, new BooleanType(false)));
                    fhirClientService.update(basic, getPracticeId());
                });
    }

    private String detectCardType(String cardNumber) {
        if (cardNumber == null) return "UNKNOWN";
        if (cardNumber.startsWith("4")) return "VISA";
        if (cardNumber.startsWith("5")) return "MASTERCARD";
        if (cardNumber.startsWith("3")) return "AMEX";
        if (cardNumber.startsWith("6")) return "DISCOVER";
        return "UNKNOWN";
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) return "****";
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }

    private String getStringExt(Basic basic, String url) {
        Extension ext = basic.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType) {
            return ((StringType) ext.getValue()).getValue();
        }
        return null;
    }

    private Integer getIntExt(Basic basic, String url) {
        Extension ext = basic.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof IntegerType) {
            return ((IntegerType) ext.getValue()).getValue();
        }
        return null;
    }

    private Long getLongExt(Basic basic, String url) {
        Extension ext = basic.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType) {
            try {
                return Long.parseLong(((StringType) ext.getValue()).getValue());
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private Boolean getBoolExt(Basic basic, String url) {
        Extension ext = basic.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof BooleanType) {
            return ((BooleanType) ext.getValue()).booleanValue();
        }
        return null;
    }
}
