package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ListOptionDto;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR-only ListOption Service (UI dropdown options).
 * Uses FHIR Basic resource with extensions for list option data.
 * No local database storage - all data stored in FHIR server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ListOptionService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private static final String LIST_OPTION_TYPE_SYSTEM = "http://ciyex.com/fhir/resource-type";
    private static final String LIST_OPTION_TYPE_CODE = "list-option";
    private static final String EXT_LIST_ID = "http://ciyex.com/fhir/StructureDefinition/list-id";
    private static final String EXT_OPTION_ID = "http://ciyex.com/fhir/StructureDefinition/option-id";
    private static final String EXT_TITLE = "http://ciyex.com/fhir/StructureDefinition/title";
    private static final String EXT_SEQ = "http://ciyex.com/fhir/StructureDefinition/sequence";
    private static final String EXT_IS_DEFAULT = "http://ciyex.com/fhir/StructureDefinition/is-default";
    private static final String EXT_OPTION_VALUE = "http://ciyex.com/fhir/StructureDefinition/option-value";
    private static final String EXT_NOTES = "http://ciyex.com/fhir/StructureDefinition/notes";
    private static final String EXT_CODES = "http://ciyex.com/fhir/StructureDefinition/codes";
    private static final String EXT_ACTIVITY = "http://ciyex.com/fhir/StructureDefinition/activity";
    private static final String EXT_EDIT_OPTIONS = "http://ciyex.com/fhir/StructureDefinition/edit-options";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // CREATE
    public ListOptionDto create(ListOptionDto dto) {
        log.debug("Creating FHIR Basic (ListOption): listId={}, title={}", dto.getListId(), dto.getTitle());

        Basic basic = toFhirBasic(dto);
        var outcome = fhirClientService.create(basic, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setId((long) Math.abs(fhirId.hashCode()));
        dto.setTimestamp(LocalDateTime.now());
        dto.setLastUpdated(LocalDateTime.now());

        log.info("Created FHIR Basic (ListOption) with id: {}", fhirId);
        return dto;
    }

    // UPDATE
    public ListOptionDto update(String fhirId, ListOptionDto dto) {
        log.debug("Updating ListOption: {}", fhirId);

        Basic basic = toFhirBasic(dto);
        basic.setId(fhirId);
        fhirClientService.update(basic, getPracticeId());

        dto.setLastUpdated(LocalDateTime.now());
        return dto;
    }

    // DELETE
    public void delete(String fhirId) {
        log.debug("Deleting ListOption: {}", fhirId);
        fhirClientService.delete(Basic.class, fhirId, getPracticeId());
    }

    // GET ONE
    public ListOptionDto get(String fhirId) {
        log.debug("Getting ListOption: {}", fhirId);
        Basic basic = fhirClientService.read(Basic.class, fhirId, getPracticeId());
        return fromFhirBasic(basic);
    }

    // GET ALL
    public List<ListOptionDto> getAll() {
        log.debug("Getting all ListOptions");
        Bundle bundle = fhirClientService.search(Basic.class, getPracticeId());

        return fhirClientService.extractResources(bundle, Basic.class).stream()
                .filter(this::isListOption)
                .map(this::fromFhirBasic)
                .collect(Collectors.toList());
    }

    // GET BY LIST ID
    public List<ListOptionDto> getListOptionsByListId(String listId) {
        log.debug("Getting ListOptions for listId: {}", listId);
        Bundle bundle = fhirClientService.search(Basic.class, getPracticeId());

        return fhirClientService.extractResources(bundle, Basic.class).stream()
                .filter(this::isListOption)
                .map(this::fromFhirBasic)
                .filter(dto -> listId.equals(dto.getListId()))
                .collect(Collectors.toList());
    }

    // DELETE BY LIST ID
    public void deleteByListId(String listId) {
        log.debug("Deleting all ListOptions for listId: {}", listId);
        Bundle bundle = fhirClientService.search(Basic.class, getPracticeId());

        fhirClientService.extractResources(bundle, Basic.class).stream()
                .filter(this::isListOption)
                .filter(basic -> listId.equals(getListIdFromBasic(basic)))
                .forEach(basic -> {
                    String id = basic.getIdElement().getIdPart();
                    fhirClientService.delete(Basic.class, id, getPracticeId());
                    log.debug("Deleted ListOption: {}", id);
                });
    }

    // -------- FHIR Mapping --------

    private Basic toFhirBasic(ListOptionDto dto) {
        Basic basic = new Basic();

        // Code to identify as list option
        CodeableConcept code = new CodeableConcept();
        code.addCoding().setSystem(LIST_OPTION_TYPE_SYSTEM).setCode(LIST_OPTION_TYPE_CODE).setDisplay("List Option");
        basic.setCode(code);

        // List ID
        if (dto.getListId() != null) {
            basic.addExtension(new Extension(EXT_LIST_ID, new StringType(dto.getListId())));
        }

        // Option ID
        if (dto.getOptionId() != null) {
            basic.addExtension(new Extension(EXT_OPTION_ID, new StringType(dto.getOptionId())));
        }

        // Title
        if (dto.getTitle() != null) {
            basic.addExtension(new Extension(EXT_TITLE, new StringType(dto.getTitle())));
        }

        // Sequence
        if (dto.getSeq() != null) {
            basic.addExtension(new Extension(EXT_SEQ, new IntegerType(dto.getSeq())));
        }

        // Is Default
        if (dto.getIsDefault() != null) {
            basic.addExtension(new Extension(EXT_IS_DEFAULT, new BooleanType(dto.getIsDefault())));
        }

        // Option Value
        if (dto.getOptionValue() != null) {
            basic.addExtension(new Extension(EXT_OPTION_VALUE, new DecimalType(BigDecimal.valueOf(dto.getOptionValue()))));
        }

        // Notes
        if (dto.getNotes() != null) {
            basic.addExtension(new Extension(EXT_NOTES, new StringType(dto.getNotes())));
        }

        // Codes
        if (dto.getCodes() != null) {
            basic.addExtension(new Extension(EXT_CODES, new StringType(dto.getCodes())));
        }

        // Activity
        if (dto.getActivity() != null) {
            basic.addExtension(new Extension(EXT_ACTIVITY, new IntegerType(dto.getActivity())));
        }

        // Edit Options
        if (dto.getEditOptions() != null) {
            basic.addExtension(new Extension(EXT_EDIT_OPTIONS, new BooleanType(dto.getEditOptions())));
        }

        return basic;
    }

    private ListOptionDto fromFhirBasic(Basic basic) {
        ListOptionDto dto = new ListOptionDto();

        String fhirId = basic.getIdElement().getIdPart();
        dto.setId((long) Math.abs(fhirId.hashCode()));

        // List ID
        dto.setListId(getStringExt(basic, EXT_LIST_ID));

        // Option ID
        dto.setOptionId(getStringExt(basic, EXT_OPTION_ID));

        // Title
        dto.setTitle(getStringExt(basic, EXT_TITLE));

        // Sequence
        dto.setSeq(getIntExt(basic, EXT_SEQ));

        // Is Default
        dto.setIsDefault(getBoolExt(basic, EXT_IS_DEFAULT));

        // Option Value
        Extension optValExt = basic.getExtensionByUrl(EXT_OPTION_VALUE);
        if (optValExt != null && optValExt.getValue() instanceof DecimalType) {
            dto.setOptionValue(((DecimalType) optValExt.getValue()).getValue().floatValue());
        }

        // Notes
        dto.setNotes(getStringExt(basic, EXT_NOTES));

        // Codes
        dto.setCodes(getStringExt(basic, EXT_CODES));

        // Activity
        dto.setActivity(getIntExt(basic, EXT_ACTIVITY));

        // Edit Options
        dto.setEditOptions(getBoolExt(basic, EXT_EDIT_OPTIONS));

        // Timestamps
        dto.setTimestamp(LocalDateTime.now());
        dto.setLastUpdated(LocalDateTime.now());

        return dto;
    }

    private boolean isListOption(Basic basic) {
        if (!basic.hasCode()) return false;
        return basic.getCode().getCoding().stream()
                .anyMatch(c -> LIST_OPTION_TYPE_SYSTEM.equals(c.getSystem()) && LIST_OPTION_TYPE_CODE.equals(c.getCode()));
    }

    private String getListIdFromBasic(Basic basic) {
        Extension ext = basic.getExtensionByUrl(EXT_LIST_ID);
        if (ext != null && ext.getValue() instanceof StringType) {
            return ((StringType) ext.getValue()).getValue();
        }
        return null;
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

    private Boolean getBoolExt(Basic basic, String url) {
        Extension ext = basic.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof BooleanType) {
            return ((BooleanType) ext.getValue()).booleanValue();
        }
        return null;
    }
}
