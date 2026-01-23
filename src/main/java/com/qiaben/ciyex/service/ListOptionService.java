package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ListOptionDto;
import com.qiaben.ciyex.exception.ResourceNotFoundException;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
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

    // Expose practice ID for debugging
    public String getCurrentPracticeId() {
        return getPracticeId();
    }

    // GET ALL LIST IDS
    public List<String> getAllListIds() {
        String practiceId = getPracticeId();
        Bundle bundle = fhirClientService.search(Basic.class, practiceId);
        
        return fhirClientService.extractResources(bundle, Basic.class).stream()
                .filter(basic -> basic.getExtensionByUrl(EXT_LIST_ID) != null)
                .map(this::fromFhirBasic)
                .map(ListOptionDto::getListId)
                .distinct()
                .collect(Collectors.toList());
    }

    // Debug method to see what's in FHIR
    public Map<String, Object> debugFhirSearch() {
        String practiceId = getPracticeId();
        Bundle bundle = fhirClientService.search(Basic.class, practiceId);
        
        List<Basic> allBasics = fhirClientService.extractResources(bundle, Basic.class);
        List<Map<String, Object>> basicDetails = allBasics.stream()
                .map(basic -> {
                    Map<String, Object> details = new java.util.HashMap<>();
                    details.put("id", basic.getIdElement().getIdPart());
                    details.put("hasCode", basic.hasCode());
                    if (basic.hasCode()) {
                        details.put("codings", basic.getCode().getCoding().stream()
                                .map(c -> c.getSystem() + "|" + c.getCode())
                                .collect(Collectors.toList()));
                    }
                    details.put("isListOption", isListOption(basic));
                    return details;
                })
                .collect(Collectors.toList());
        
        return Map.of(
                "practiceId", practiceId,
                "totalEntries", bundle.hasEntry() ? bundle.getEntry().size() : 0,
                "basicResources", allBasics.size(),
                "basicDetails", basicDetails
        );
    }

    // CREATE
    public ListOptionDto create(ListOptionDto dto) {
        log.info("Creating FHIR Basic (ListOption): listId={}, title={}", dto.getListId(), dto.getTitle());

        Basic basic = toFhirBasic(dto);
        
        // Log the code being set
        if (basic.hasCode()) {
            basic.getCode().getCoding().forEach(c -> 
                log.info("Setting code on new resource: system='{}' code='{}'", c.getSystem(), c.getCode()));
        }
        
        var outcome = fhirClientService.create(basic, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        try {
            dto.setId(Long.parseLong(fhirId));
        } catch (NumberFormatException e) {
            dto.setId((long) Math.abs(fhirId.hashCode()));
        }
        dto.setTimestamp(LocalDateTime.now());
        dto.setLastUpdated(LocalDateTime.now());

        log.info("Created FHIR Basic (ListOption) with id: {}", fhirId);
        
        // Verify the resource was created with correct code by reading it back
        try {
            Basic created = fhirClientService.read(Basic.class, fhirId, getPracticeId());
            if (created.hasCode()) {
                created.getCode().getCoding().forEach(c -> 
                    log.info("Verified created resource {} has code: system='{}' code='{}'", 
                        fhirId, c.getSystem(), c.getCode()));
            } else {
                log.warn("Created resource {} has NO CODE!", fhirId);
            }
        } catch (Exception e) {
            log.error("Failed to verify created resource: {}", e.getMessage());
        }
        
        return dto;
    }

    // UPDATE
    public ListOptionDto update(String id, ListOptionDto dto) {
        log.debug("Updating ListOption: {}", id);

        var existingResource = fhirClientService.readOptional(Basic.class, id, getPracticeId());
        if (existingResource.isEmpty()) {
            throw new ResourceNotFoundException("ListOption not found with ID: " + id);
        }

        Basic basic = toFhirBasic(dto);
        basic.setId(id);
        fhirClientService.update(basic, getPracticeId());

        dto.setLastUpdated(LocalDateTime.now());
        return dto;
    }

    // DELETE
    public void delete(String id) {
        log.debug("Deleting ListOption: {}", id);
        fhirClientService.delete(Basic.class, id, getPracticeId());
    }

    // GET ONE
    public ListOptionDto get(String id) {
        log.debug("Getting ListOption: {}", id);
        
        var optionalBasic = fhirClientService.readOptional(Basic.class, id, getPracticeId());
        if (optionalBasic.isEmpty()) {
            throw new ResourceNotFoundException("ListOption not found with ID: " + id);
        }
        
        return fromFhirBasic(optionalBasic.get());
    }

    // GET ALL
    public List<ListOptionDto> getAll() {
        String practiceId = getPracticeId();
        log.info("Getting all ListOptions for practiceId: '{}'", practiceId);
        
        // Search ALL Basic resources without code filter
        Bundle bundle = fhirClientService.search(Basic.class, practiceId);
        log.info("FHIR search returned bundle with {} entries", 
                bundle.hasEntry() ? bundle.getEntry().size() : 0);

        // Filter by checking if it has the list-id extension (more reliable than code)
        List<ListOptionDto> result = fhirClientService.extractResources(bundle, Basic.class).stream()
                .filter(basic -> basic.getExtensionByUrl(EXT_LIST_ID) != null)
                .map(this::fromFhirBasic)
                .collect(Collectors.toList());
        
        log.info("Converted {} ListOptions to DTOs", result.size());
        return result;
    }

    // GET BY LIST ID
    public List<ListOptionDto> getListOptionsByListId(String listId) {
        String practiceId = getPracticeId();
        log.info("Getting ListOptions for listId: {} and practiceId: {}", listId, practiceId);
        
        Bundle bundle = fhirClientService.search(Basic.class, practiceId);
        log.info("FHIR search returned bundle with {} total entries", 
                bundle.hasEntry() ? bundle.getEntry().size() : 0);

        List<Basic> allBasics = fhirClientService.extractResources(bundle, Basic.class);
        log.info("Extracted {} Basic resources", allBasics.size());
        
        // Count how many have the extension
        long withExtension = allBasics.stream()
                .filter(basic -> basic.getExtensionByUrl(EXT_LIST_ID) != null)
                .count();
        log.info("{} resources have EXT_LIST_ID extension", withExtension);
        
        List<ListOptionDto> result = allBasics.stream()
                .filter(basic -> basic.getExtensionByUrl(EXT_LIST_ID) != null)
                .map(this::fromFhirBasic)
                .peek(dto -> log.info("Found ListOption: id={}, listId={}, title={}", dto.getId(), dto.getListId(), dto.getTitle()))
                .filter(dto -> listId.equals(dto.getListId()))
                .collect(Collectors.toList());
        
        log.info("Found {} ListOptions for listId: {}", result.size(), listId);
        return result;
    }

    // DELETE BY LIST ID
    public void deleteByListId(String listId) {
        log.debug("Deleting all ListOptions for listId: {}", listId);
        String practiceId = getPracticeId();
        
        Bundle bundle = fhirClientService.search(Basic.class, practiceId);

        fhirClientService.extractResources(bundle, Basic.class).stream()
                .filter(basic -> basic.getExtensionByUrl(EXT_LIST_ID) != null)
                .map(this::fromFhirBasic)
                .filter(dto -> listId.equals(dto.getListId()))
                .forEach(dto -> {
                    fhirClientService.delete(Basic.class, String.valueOf(dto.getId()), practiceId);
                    log.debug("Deleted ListOption: {}", dto.getId());
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
        try {
            dto.setId(Long.parseLong(fhirId));
        } catch (NumberFormatException e) {
            dto.setId((long) Math.abs(fhirId.hashCode()));
        }

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
        String basicId = basic.getIdElement().getIdPart();
        
        if (!basic.hasCode()) {
            log.info("Basic resource {} has no code - NOT a ListOption", basicId);
            return false;
        }
        
        // Log all codings in this resource
        basic.getCode().getCoding().forEach(c -> 
            log.info("Basic resource {} has coding: system='{}' code='{}'", 
                basicId, c.getSystem(), c.getCode()));
        
        boolean isListOption = basic.getCode().getCoding().stream()
                .anyMatch(c -> LIST_OPTION_TYPE_SYSTEM.equals(c.getSystem()) && LIST_OPTION_TYPE_CODE.equals(c.getCode()));
        
        log.info("Basic resource {} isListOption={}", basicId, isListOption);
        
        return isListOption;
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
