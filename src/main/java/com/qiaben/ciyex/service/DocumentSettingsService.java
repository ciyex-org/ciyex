package com.qiaben.ciyex.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiaben.ciyex.dto.DocumentSettingsDto;
// import com.qiaben.ciyex.dto.integration.RequestContext; // not used here
import com.qiaben.ciyex.entity.DocumentSettings;
import com.qiaben.ciyex.repository.DocumentSettingsRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentSettingsService {

    private final DocumentSettingsRepo repository;
    private final ObjectMapper objectMapper;

    /**
     * Get document settings for an organization
     */
    public DocumentSettingsDto get() {
        DocumentSettings entity = repository.findFirstByOrderByIdAsc()
            .orElseGet(() -> {
                DocumentSettings created = createDefaultSettings();
                return repository.save(created);
            });

        return toDto(entity);
    }
    /**
     * Create or update document settings
     */
    @Transactional
    public DocumentSettingsDto save(DocumentSettingsDto dto, String updatedBy) {
        // Upsert: load existing or create a new default entity if none persisted yet
        DocumentSettings entity = repository.findFirstByOrderByIdAsc().orElse(null);
        boolean isNew = (entity == null);
        if (entity == null) {
            entity = createDefaultSettings();
        }

        // Apply incoming values (override defaults)
        entity.setMaxUploadBytes(dto.getMaxUploadSizeMB() * 1024L * 1024L);
        entity.setEnableAudio(dto.isEnableAudio());
        entity.setEncryptionEnabled(dto.isEncryptionEnabled());

        // Ensure JPG and JPEG are always together
        List<String> fileTypes = new ArrayList<>(dto.getAllowedFileTypes());
        boolean hasJpg = fileTypes.stream().anyMatch(ft -> ft.equalsIgnoreCase("JPG"));
        boolean hasJpeg = fileTypes.stream().anyMatch(ft -> ft.equalsIgnoreCase("JPEG"));

        if (hasJpg && !hasJpeg) {
            fileTypes.add("JPEG");
        } else if (hasJpeg && !hasJpg) {
            fileTypes.add("JPG");
        }

        try {
            entity.setAllowedFileTypesJson(objectMapper.writeValueAsString(fileTypes));
            entity.setCategoriesJson(objectMapper.writeValueAsString(dto.getCategories()));
        } catch (JsonProcessingException e) {
            log.error("Error serializing document settings", e);
            throw new RuntimeException("Failed to save document settings", e);
        }

        entity.setUpdatedBy(updatedBy);
        entity.setUpdatedAt(Instant.now());

        DocumentSettings saved = repository.save(entity);
        log.debug("DocumentSettings {} (id={}) saved", isNew ? "created" : "updated", saved.getId());
        return toDto(saved);
    }

    /**
     * Get all document settings
     */
    public List<DocumentSettingsDto> getAll() {
        return repository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Get categories for an organization
     */
    public List<DocumentSettingsDto.Category> getCategories() {
        DocumentSettingsDto dto = get();
        return dto.getCategories();
    }

    /**
     * Add a category to an organization's document settings
     */
    @Transactional
    public List<DocumentSettingsDto.Category> addCategory(String name, boolean active, String updatedBy) {
        DocumentSettingsDto dto = get();
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Category name cannot be empty");
        }

        // Prevent duplicates (case-insensitive); if exists, just update active flag
        boolean updatedExisting = false;
        for (DocumentSettingsDto.Category c : dto.getCategories()) {
            if (c.getName().equalsIgnoreCase(name.trim())) {
                c.setActive(active);
                updatedExisting = true;
                break;
            }
        }
        if (!updatedExisting) {
            DocumentSettingsDto.Category newCategory = new DocumentSettingsDto.Category(name.trim(), active);
            dto.getCategories().add(newCategory);
        }

        // Persist changes via upsert save
        save(dto, updatedBy);
        return dto.getCategories();
    }

    /**
     * Delete a specific category by name
     */
    @Transactional
    public List<DocumentSettingsDto.Category> deleteCategory(String name, String updatedBy) {
        DocumentSettingsDto dto = get();

        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Category name cannot be empty");
        }

        // Remove category (case-insensitive)
        boolean removed = dto.getCategories().removeIf(
                c -> c.getName().equalsIgnoreCase(name.trim())
        );

        if (!removed) {
            throw new RuntimeException("Category not found: " + name);
        }

        // Persist changes
        save(dto, updatedBy);
        return dto.getCategories();
    }

    /**
     * Delete all categories
     */
    @Transactional
    public List<DocumentSettingsDto.Category> deleteAllCategories(String updatedBy) {
        DocumentSettingsDto dto = get();
        dto.getCategories().clear();

        // Persist changes
        save(dto, updatedBy);
        return dto.getCategories();
    }

    // Private helper methods

    private DocumentSettings createDefaultSettings() {
        DocumentSettings settings = new DocumentSettings();
        settings.setMaxUploadBytes(10 * 1024 * 1024); // 10 MB default
        settings.setEnableAudio(false);
        settings.setEncryptionEnabled(false);

        try {
            settings.setAllowedFileTypesJson(objectMapper.writeValueAsString(
                List.of("PDF", "JPG", "JPEG", "PNG", "DOC", "DOCX", "TXT")));
            settings.setCategoriesJson(objectMapper.writeValueAsString(
                    List.of(
                            new DocumentSettingsDto.Category("Medical Records", true),
                            new DocumentSettingsDto.Category("Lab Results", true)
                    )));
        } catch (JsonProcessingException e) {
            log.error("Error creating default settings", e);
        }

        return settings;
    }

    private DocumentSettingsDto toDto(DocumentSettings entity) {
        DocumentSettingsDto dto = new DocumentSettingsDto();
        dto.setMaxUploadSizeMB((int) (entity.getMaxUploadBytes() / (1024 * 1024)));
        dto.setEnableAudio(entity.isEnableAudio());
        dto.setEncryptionEnabled(entity.isEncryptionEnabled());

        try {
            dto.setAllowedFileTypes(objectMapper.readValue(
                    entity.getAllowedFileTypesJson(),
                    new TypeReference<List<String>>() {}));
            dto.setCategories(objectMapper.readValue(
                    entity.getCategoriesJson(),
                    new TypeReference<List<DocumentSettingsDto.Category>>() {}));
        } catch (JsonProcessingException e) {
            log.error("Error deserializing document settings", e);
            dto.setAllowedFileTypes(new ArrayList<>());
            dto.setCategories(new ArrayList<>());
        }

        return dto;
    }
}