package org.ciyex.ehr.service;

import org.ciyex.ehr.dto.GpsBillingCardDto;
import org.ciyex.ehr.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * FHIR-only GPS Billing Card Service.
 * Uses FHIR Basic resource for storing payment card tokens.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GpsBillingCardService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private static final String CARD_TYPE_SYSTEM = "http://ciyex.com/fhir/resource-type";
    private static final String CARD_TYPE_CODE = "gps-billing-card";
    private static final String EXT_USER_ID = "http://ciyex.com/fhir/StructureDefinition/user-id";
    private static final String EXT_GPS_VAULT_ID = "http://ciyex.com/fhir/StructureDefinition/gps-customer-vault-id";
    private static final String EXT_BRAND = "http://ciyex.com/fhir/StructureDefinition/brand";
    private static final String EXT_LAST4 = "http://ciyex.com/fhir/StructureDefinition/last4";
    private static final String EXT_EXP_MONTH = "http://ciyex.com/fhir/StructureDefinition/exp-month";
    private static final String EXT_EXP_YEAR = "http://ciyex.com/fhir/StructureDefinition/exp-year";
    private static final String EXT_IS_DEFAULT = "http://ciyex.com/fhir/StructureDefinition/is-default";
    private static final String EXT_FIRST_NAME = "http://ciyex.com/fhir/StructureDefinition/first-name";
    private static final String EXT_LAST_NAME = "http://ciyex.com/fhir/StructureDefinition/last-name";
    private static final String EXT_ADDRESS = "http://ciyex.com/fhir/StructureDefinition/address";
    private static final String EXT_STREET = "http://ciyex.com/fhir/StructureDefinition/street";
    private static final String EXT_CITY = "http://ciyex.com/fhir/StructureDefinition/city";
    private static final String EXT_STATE = "http://ciyex.com/fhir/StructureDefinition/state";
    private static final String EXT_ZIP = "http://ciyex.com/fhir/StructureDefinition/zip";
    private static final String EXT_CREATED_AT = "http://ciyex.com/fhir/StructureDefinition/created-at";
    private static final String EXT_UPDATED_AT = "http://ciyex.com/fhir/StructureDefinition/updated-at";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    /* ---------------- CREATE ---------------- */
    public GpsBillingCardDto create(GpsBillingCardDto dto) {
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());

        if (dto.isDefault()) {
            clearDefaultForUser(dto.getUserId());
        }

        // Tokenize card if gpsCustomerVaultId not provided
        if (dto.getGpsCustomerVaultId() == null || dto.getGpsCustomerVaultId().isBlank()) {
            dto.setGpsCustomerVaultId(gpsTokenize(dto));
        }

        Basic basic = toFhirBasic(dto);
        var outcome = fhirClientService.create(basic, getPracticeId());
        String fhirId = outcome.getId().getIdPart();
        dto.setId((long) Math.abs(fhirId.hashCode()));

        log.info("Created GPS billing card with FHIR ID: {}", fhirId);
        return dto;
    }

    /* ---------------- READ ---------------- */
    public List<GpsBillingCardDto> getAll() {
        Bundle bundle = fhirClientService.search(Basic.class, getPracticeId());
        return fhirClientService.extractResources(bundle, Basic.class).stream()
                .filter(this::isGpsBillingCard)
                .map(this::fromFhirBasic)
                .collect(Collectors.toList());
    }

    public List<GpsBillingCardDto> getAllByUser(UUID userId) {
        Bundle bundle = fhirClientService.search(Basic.class, getPracticeId());
        return fhirClientService.extractResources(bundle, Basic.class).stream()
                .filter(this::isGpsBillingCard)
                .filter(b -> userId.toString().equals(getStringExt(b, EXT_USER_ID)))
                .map(this::fromFhirBasic)
                .collect(Collectors.toList());
    }

    public Optional<GpsBillingCardDto> getById(Long id) {
        Bundle bundle = fhirClientService.search(Basic.class, getPracticeId());
        return fhirClientService.extractResources(bundle, Basic.class).stream()
                .filter(this::isGpsBillingCard)
                .filter(b -> id.equals((long) Math.abs(b.getIdElement().getIdPart().hashCode())))
                .map(this::fromFhirBasic)
                .findFirst();
    }

    /* ---------------- UPDATE ---------------- */
    public GpsBillingCardDto update(Long id, GpsBillingCardDto dto) {
        Bundle bundle = fhirClientService.search(Basic.class, getPracticeId());
        Basic existing = fhirClientService.extractResources(bundle, Basic.class).stream()
                .filter(this::isGpsBillingCard)
                .filter(b -> id.equals((long) Math.abs(b.getIdElement().getIdPart().hashCode())))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Card not found"));

        String fhirId = existing.getIdElement().getIdPart();
        dto.setUpdatedAt(LocalDateTime.now());

        if (dto.isDefault()) {
            clearDefaultForUser(dto.getUserId());
        }

        Basic updated = toFhirBasic(dto);
        updated.setId(fhirId);
        fhirClientService.update(updated, getPracticeId());

        dto.setId(id);
        return dto;
    }

    /* ---------------- DELETE ---------------- */
    public void delete(Long id) {
        Bundle bundle = fhirClientService.search(Basic.class, getPracticeId());
        fhirClientService.extractResources(bundle, Basic.class).stream()
                .filter(this::isGpsBillingCard)
                .filter(b -> id.equals((long) Math.abs(b.getIdElement().getIdPart().hashCode())))
                .findFirst()
                .ifPresent(b -> fhirClientService.delete(Basic.class, b.getIdElement().getIdPart(), getPracticeId()));
    }

    /* ---------------- SET DEFAULT ---------------- */
    public GpsBillingCardDto setDefault(Long id) {
        GpsBillingCardDto card = getById(id)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        clearDefaultForUser(card.getUserId());
        card.setDefault(true);
        card.setUpdatedAt(LocalDateTime.now());

        return update(id, card);
    }

    private void clearDefaultForUser(UUID userId) {
        if (userId == null) return;
        getAllByUser(userId).stream()
                .filter(GpsBillingCardDto::isDefault)
                .forEach(c -> {
                    c.setDefault(false);
                    update(c.getId(), c);
                });
    }

    /* ---------------- MOCK GPS TOKENIZE ---------------- */
    private String gpsTokenize(GpsBillingCardDto dto) {
        if (dto.getCardNumber() != null && dto.getCvv() != null) {
            return "GPS-" + dto.getCardNumber().substring(dto.getCardNumber().length() - 4)
                    + "-" + System.currentTimeMillis();
        }
        return "MOCK-" + System.currentTimeMillis();
    }

    /* ---------------- FHIR MAPPERS ---------------- */
    private Basic toFhirBasic(GpsBillingCardDto dto) {
        Basic basic = new Basic();

        CodeableConcept code = new CodeableConcept();
        code.addCoding().setSystem(CARD_TYPE_SYSTEM).setCode(CARD_TYPE_CODE).setDisplay("GPS Billing Card");
        basic.setCode(code);

        if (dto.getUserId() != null) basic.addExtension(new Extension(EXT_USER_ID, new StringType(dto.getUserId().toString())));
        if (dto.getGpsCustomerVaultId() != null) basic.addExtension(new Extension(EXT_GPS_VAULT_ID, new StringType(dto.getGpsCustomerVaultId())));
        if (dto.getBrand() != null) basic.addExtension(new Extension(EXT_BRAND, new StringType(dto.getBrand())));
        if (dto.getLast4() != null) basic.addExtension(new Extension(EXT_LAST4, new StringType(dto.getLast4())));
        if (dto.getExpMonth() != null) basic.addExtension(new Extension(EXT_EXP_MONTH, new StringType(dto.getExpMonth().toString())));
        if (dto.getExpYear() != null) basic.addExtension(new Extension(EXT_EXP_YEAR, new StringType(dto.getExpYear().toString())));
        basic.addExtension(new Extension(EXT_IS_DEFAULT, new BooleanType(dto.isDefault())));
        if (dto.getFirstName() != null) basic.addExtension(new Extension(EXT_FIRST_NAME, new StringType(dto.getFirstName())));
        if (dto.getLastName() != null) basic.addExtension(new Extension(EXT_LAST_NAME, new StringType(dto.getLastName())));
        if (dto.getAddress() != null) basic.addExtension(new Extension(EXT_ADDRESS, new StringType(dto.getAddress())));
        if (dto.getStreet() != null) basic.addExtension(new Extension(EXT_STREET, new StringType(dto.getStreet())));
        if (dto.getCity() != null) basic.addExtension(new Extension(EXT_CITY, new StringType(dto.getCity())));
        if (dto.getState() != null) basic.addExtension(new Extension(EXT_STATE, new StringType(dto.getState())));
        if (dto.getZip() != null) basic.addExtension(new Extension(EXT_ZIP, new StringType(dto.getZip())));
        if (dto.getCreatedAt() != null) basic.addExtension(new Extension(EXT_CREATED_AT, new DateTimeType(Date.from(dto.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant()))));
        if (dto.getUpdatedAt() != null) basic.addExtension(new Extension(EXT_UPDATED_AT, new DateTimeType(Date.from(dto.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant()))));

        return basic;
    }

    private GpsBillingCardDto fromFhirBasic(Basic basic) {
        String fhirId = basic.getIdElement().getIdPart();

        GpsBillingCardDto.GpsBillingCardDtoBuilder builder = GpsBillingCardDto.builder()
                .id((long) Math.abs(fhirId.hashCode()))
                .gpsCustomerVaultId(getStringExt(basic, EXT_GPS_VAULT_ID))
                .brand(getStringExt(basic, EXT_BRAND))
                .last4(getStringExt(basic, EXT_LAST4))
                .expMonth(parseInteger(getStringExt(basic, EXT_EXP_MONTH)))
                .expYear(parseInteger(getStringExt(basic, EXT_EXP_YEAR)))
                .isDefault(getBoolExt(basic, EXT_IS_DEFAULT))
                .firstName(getStringExt(basic, EXT_FIRST_NAME))
                .lastName(getStringExt(basic, EXT_LAST_NAME))
                .address(getStringExt(basic, EXT_ADDRESS))
                .street(getStringExt(basic, EXT_STREET))
                .city(getStringExt(basic, EXT_CITY))
                .state(getStringExt(basic, EXT_STATE))
                .zip(getStringExt(basic, EXT_ZIP));

        String userIdStr = getStringExt(basic, EXT_USER_ID);
        if (userIdStr != null) {
            try { builder.userId(UUID.fromString(userIdStr)); } catch (Exception ignored) {}
        }

        Extension createdExt = basic.getExtensionByUrl(EXT_CREATED_AT);
        if (createdExt != null && createdExt.getValue() instanceof DateTimeType) {
            Date date = ((DateTimeType) createdExt.getValue()).getValue();
            if (date != null) builder.createdAt(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
        }

        Extension updatedExt = basic.getExtensionByUrl(EXT_UPDATED_AT);
        if (updatedExt != null && updatedExt.getValue() instanceof DateTimeType) {
            Date date = ((DateTimeType) updatedExt.getValue()).getValue();
            if (date != null) builder.updatedAt(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
        }

        return builder.build();
    }

    private boolean isGpsBillingCard(Basic basic) {
        if (!basic.hasCode()) return false;
        return basic.getCode().getCoding().stream()
                .anyMatch(c -> CARD_TYPE_SYSTEM.equals(c.getSystem()) && CARD_TYPE_CODE.equals(c.getCode()));
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

    private Integer parseInteger(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
