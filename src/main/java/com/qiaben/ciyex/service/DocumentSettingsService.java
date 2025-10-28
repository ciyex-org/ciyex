package com.qiaben.ciyex.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiaben.ciyex.dto.DocumentSettingsDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
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
                .orElse(createDefaultSettings());
        
        return toDto(entity);
    }
    /**
     * Create or update document settings
     */
    @Transactional
    public DocumentSettingsDto save(DocumentSettingsDto dto, String updatedBy) {
        //TODO: Fix
        DocumentSettings entity = repository.findFirstByOrderByIdAsc().orElseThrow(() -> new RuntimeException("Settings not found"));
        
        entity.setMaxUploadBytes(dto.getMaxUploadSizeMB() * 1024L * 1024L);
        entity.setEnableAudio(dto.isEnableAudio());
        entity.setEncryptionEnabled(dto.isEncryptionEnabled());
        
        try {
            entity.setAllowedFileTypesJson(objectMapper.writeValueAsString(dto.getAllowedFileTypes()));
            entity.setCategoriesJson(objectMapper.writeValueAsString(dto.getCategories()));
        } catch (JsonProcessingException e) {
            log.error("Error serializing document settings", e);
            throw new RuntimeException("Failed to save document settings", e);
        }
        
        entity.setUpdatedBy(updatedBy);
        entity.setUpdatedAt(Instant.now());
        
        DocumentSettings saved = repository.save(entity);
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
        
        // Add new category
        DocumentSettingsDto.Category newCategory = new DocumentSettingsDto.Category(name, active);
        dto.getCategories().add(newCategory);
        
        // Save updated settings
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
                    List.of("PDF", "JPG", "JPEG", "PNG", "DOC", "DOCX")));
            settings.setCategoriesJson(objectMapper.writeValueAsString(
                    List.of(
                            new DocumentSettingsDto.Category("Medical Records", true),
                            new DocumentSettingsDto.Category("Lab Results", true),
                            new DocumentSettingsDto.Category("Insurance", true),
                            new DocumentSettingsDto.Category("Other", true)
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
