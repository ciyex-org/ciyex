package org.ciyex.ehr.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ciyex.ehr.dto.DocumentSettingsDto;
import org.ciyex.ehr.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR-only Document Settings Service.
 * Uses FHIR Basic resource for storing document settings.
 * No local database storage - all data stored in FHIR server.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentSettingsService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;
    private final ObjectMapper objectMapper;
    
    // Simple cache invalidation flag
    private volatile boolean cacheInvalidated = false;

    private static final String SETTINGS_TYPE_SYSTEM = "http://ciyex.com/fhir/resource-type";
    private static final String SETTINGS_TYPE_CODE = "document-settings";
    private static final String EXT_MAX_UPLOAD = "http://ciyex.com/fhir/StructureDefinition/max-upload-bytes";
    private static final String EXT_ENABLE_AUDIO = "http://ciyex.com/fhir/StructureDefinition/enable-audio";
    private static final String EXT_ENCRYPTION = "http://ciyex.com/fhir/StructureDefinition/encryption-enabled";
    private static final String EXT_FILE_TYPES = "http://ciyex.com/fhir/StructureDefinition/allowed-file-types";
    private static final String EXT_CATEGORIES = "http://ciyex.com/fhir/StructureDefinition/categories";

    private String getPracticeId() {
        String practiceId = practiceContextService.getPracticeId();
        log.debug("Current practice ID: {}", practiceId);
        return practiceId;
    }

    // GET (creates default if not exists)
    public DocumentSettingsDto get() {
        log.debug("Getting document settings");

        Bundle bundle = fhirClientService.search(Basic.class, getPracticeId());
        List<Basic> allBasics = fhirClientService.extractResources(bundle, Basic.class);
        log.debug("Found {} Basic resources total", allBasics.size());
        
        List<Basic> settings = allBasics.stream()
                .filter(this::isDocumentSettings)
                .toList();
        log.debug("Found {} document settings resources", settings.size());

        if (!settings.isEmpty()) {
            log.debug("Returning existing document settings");
            // If multiple settings exist, delete extras and keep the latest
            if (settings.size() > 1) {
                log.warn("Found {} document settings, cleaning up old ones", settings.size());
                cleanupDuplicateSettings(settings);
            }
            return fromFhirBasic(settings.get(settings.size() - 1));
        }

        // Create default settings
        log.info("No document settings found, creating defaults");
        return save(createDefaultDto(), "system");
    }

    // SAVE (create or update)
    public DocumentSettingsDto save(DocumentSettingsDto dto, String updatedBy) {
        log.debug("Saving document settings");

        Bundle bundle = fhirClientService.search(Basic.class, getPracticeId());
        List<Basic> allBasics = fhirClientService.extractResources(bundle, Basic.class);
        log.debug("Found {} Basic resources total for save", allBasics.size());
        
        List<Basic> settings = allBasics.stream()
                .filter(this::isDocumentSettings)
                .toList();
        log.debug("Found {} document settings resources for save", settings.size());

        // Handle file types based on audio setting
        List<String> fileTypes = new ArrayList<>(dto.getAllowedFileTypes());
        
        // Add MP3 if audio is enabled and not already present
        if (dto.isEnableAudio() && fileTypes.stream().noneMatch(ft -> ft.equalsIgnoreCase("MP3"))) {
            fileTypes.add("MP3");
        }
        // Remove MP3 if audio is disabled
        else if (!dto.isEnableAudio()) {
            fileTypes.removeIf(ft -> ft.equalsIgnoreCase("MP3"));
        }
        
        // Ensure JPG and JPEG are always together
        boolean hasJpg = fileTypes.stream().anyMatch(ft -> ft.equalsIgnoreCase("JPG"));
        boolean hasJpeg = fileTypes.stream().anyMatch(ft -> ft.equalsIgnoreCase("JPEG"));
        if (hasJpg && !hasJpeg) fileTypes.add("JPEG");
        else if (hasJpeg && !hasJpg) fileTypes.add("JPG");
        
        dto.setAllowedFileTypes(fileTypes);

        Basic basic = toFhirBasic(dto);

        if (settings.isEmpty()) {
            // Create new
            var outcome = fhirClientService.create(basic, getPracticeId());
            String fhirId = outcome.getId().getIdPart();
            dto.setFhirId(fhirId);
            log.info("Created document settings with FHIR ID: {}", fhirId);
        } else {
            // Update existing
            String fhirId = settings.get(0).getIdElement().getIdPart();
            basic.setId(fhirId);
            dto.setFhirId(fhirId);
            fhirClientService.update(basic, getPracticeId());
            log.info("Updated document settings with FHIR ID: {}", fhirId);
        }
        
        // Invalidate cache after save
        cacheInvalidated = true;

        return dto;
    }

    // GET ALL
    public List<DocumentSettingsDto> getAll() {
        Bundle bundle = fhirClientService.search(Basic.class, getPracticeId());
        return fhirClientService.extractResources(bundle, Basic.class).stream()
                .filter(this::isDocumentSettings)
                .map(this::fromFhirBasic)
                .collect(Collectors.toList());
    }

    // GET CATEGORIES
    public List<DocumentSettingsDto.Category> getCategories() {
        return get().getCategories();
    }

    // ADD CATEGORY
    public List<DocumentSettingsDto.Category> addCategory(String name, boolean active, String updatedBy) {
        DocumentSettingsDto dto = get();
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Category name cannot be empty");
        }

        boolean updatedExisting = false;
        for (DocumentSettingsDto.Category c : dto.getCategories()) {
            if (c.getName().equalsIgnoreCase(name.trim())) {
                c.setActive(active);
                updatedExisting = true;
                break;
            }
        }
        if (!updatedExisting) {
            dto.getCategories().add(new DocumentSettingsDto.Category(name.trim(), active));
        }

        save(dto, updatedBy);
        return dto.getCategories();
    }

    // DELETE CATEGORY
    public List<DocumentSettingsDto.Category> deleteCategory(String name, String updatedBy) {
        DocumentSettingsDto dto = get();
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Category name cannot be empty");
        }

        boolean removed = dto.getCategories().removeIf(c -> c.getName().equalsIgnoreCase(name.trim()));
        if (!removed) {
            throw new RuntimeException("Category not found: " + name);
        }

        save(dto, updatedBy);
        return dto.getCategories();
    }

    // DELETE ALL CATEGORIES
    public List<DocumentSettingsDto.Category> deleteAllCategories(String updatedBy) {
        DocumentSettingsDto dto = get();
        dto.getCategories().clear();
        save(dto, updatedBy);
        return dto.getCategories();
    }

    // -------- FHIR Mapping --------

    private Basic toFhirBasic(DocumentSettingsDto dto) {
        Basic basic = new Basic();

        CodeableConcept code = new CodeableConcept();
        code.addCoding().setSystem(SETTINGS_TYPE_SYSTEM).setCode(SETTINGS_TYPE_CODE).setDisplay("Document Settings");
        basic.setCode(code);

        basic.addExtension(new Extension(EXT_MAX_UPLOAD, new IntegerType(dto.getMaxUploadSizeMB() * 1024 * 1024)));
        basic.addExtension(new Extension(EXT_ENABLE_AUDIO, new BooleanType(dto.isEnableAudio())));
        basic.addExtension(new Extension(EXT_ENCRYPTION, new BooleanType(dto.isEncryptionEnabled())));

        try {
            basic.addExtension(new Extension(EXT_FILE_TYPES, new StringType(objectMapper.writeValueAsString(dto.getAllowedFileTypes()))));
            basic.addExtension(new Extension(EXT_CATEGORIES, new StringType(objectMapper.writeValueAsString(dto.getCategories()))));
        } catch (JsonProcessingException e) {
            log.error("Error serializing document settings", e);
        }

        return basic;
    }

    private DocumentSettingsDto fromFhirBasic(Basic basic) {
        DocumentSettingsDto dto = new DocumentSettingsDto();
        
        // Set FHIR ID
        if (basic.hasId()) {
            String fhirId = basic.getIdElement().getIdPart();
            log.debug("Setting FHIR ID: {}", fhirId);
            dto.setFhirId(fhirId);
        } else {
            log.debug("Basic resource has no ID");
        }

        Extension maxUploadExt = basic.getExtensionByUrl(EXT_MAX_UPLOAD);
        if (maxUploadExt != null && maxUploadExt.getValue() instanceof IntegerType) {
            int bytes = ((IntegerType) maxUploadExt.getValue()).getValue();
            dto.setMaxUploadSizeMB(bytes / (1024 * 1024));
        } else {
            dto.setMaxUploadSizeMB(10);
        }

        Extension audioExt = basic.getExtensionByUrl(EXT_ENABLE_AUDIO);
        if (audioExt != null && audioExt.getValue() instanceof BooleanType) {
            dto.setEnableAudio(((BooleanType) audioExt.getValue()).booleanValue());
        }

        Extension encryptExt = basic.getExtensionByUrl(EXT_ENCRYPTION);
        if (encryptExt != null && encryptExt.getValue() instanceof BooleanType) {
            dto.setEncryptionEnabled(((BooleanType) encryptExt.getValue()).booleanValue());
        }

        try {
            Extension fileTypesExt = basic.getExtensionByUrl(EXT_FILE_TYPES);
            if (fileTypesExt != null && fileTypesExt.getValue() instanceof StringType) {
                dto.setAllowedFileTypes(objectMapper.readValue(
                        ((StringType) fileTypesExt.getValue()).getValue(),
                        new TypeReference<List<String>>() {}));
            } else {
                dto.setAllowedFileTypes(new ArrayList<>());
            }

            Extension categoriesExt = basic.getExtensionByUrl(EXT_CATEGORIES);
            if (categoriesExt != null && categoriesExt.getValue() instanceof StringType) {
                dto.setCategories(objectMapper.readValue(
                        ((StringType) categoriesExt.getValue()).getValue(),
                        new TypeReference<List<DocumentSettingsDto.Category>>() {}));
            } else {
                dto.setCategories(new ArrayList<>());
            }
        } catch (JsonProcessingException e) {
            log.error("Error deserializing document settings", e);
            dto.setAllowedFileTypes(new ArrayList<>());
            dto.setCategories(new ArrayList<>());
        }

        return dto;
    }

    private boolean isDocumentSettings(Basic basic) {
        if (!basic.hasCode()) {
            log.debug("Basic resource has no code");
            return false;
        }
        boolean isDocSettings = basic.getCode().getCoding().stream()
                .anyMatch(c -> {
                    boolean matches = SETTINGS_TYPE_SYSTEM.equals(c.getSystem()) && SETTINGS_TYPE_CODE.equals(c.getCode());
                    log.debug("Checking coding: system={}, code={}, matches={}", c.getSystem(), c.getCode(), matches);
                    return matches;
                });
        log.debug("Basic resource isDocumentSettings: {}", isDocSettings);
        return isDocSettings;
    }

    private void cleanupDuplicateSettings(List<Basic> settings) {
        // Keep the latest one (last in list), delete the old ones
        for (int i = 0; i < settings.size() - 1; i++) {
            try {
                String fhirId = settings.get(i).getIdElement().getIdPart();
                fhirClientService.delete(Basic.class, fhirId, getPracticeId());
                log.info("Deleted old document settings with FHIR ID: {}", fhirId);
            } catch (Exception e) {
                log.warn("Failed to delete old settings: {}", e.getMessage());
            }
        }
    }

    private DocumentSettingsDto createDefaultDto() {
        return DocumentSettingsDto.builder()
                .maxUploadSizeMB(10)
                .enableAudio(false)
                .encryptionEnabled(false)
                .allowedFileTypes(List.of("PDF", "JPG", "JPEG", "PNG", "DOC", "DOCX", "TXT"))
                .categories(List.of(
                        new DocumentSettingsDto.Category("Medical Records", true),
                        new DocumentSettingsDto.Category("Lab Results", true)
                ))
                .build();
    }
}
