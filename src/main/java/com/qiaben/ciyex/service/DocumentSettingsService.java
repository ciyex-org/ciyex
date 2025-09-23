package com.qiaben.ciyex.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiaben.ciyex.dto.DocumentSettingsDto;
import com.qiaben.ciyex.entity.DocumentSettings;
import com.qiaben.ciyex.repository.DocumentSettingsRepo;
import com.qiaben.ciyex.repository.OrgRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class DocumentSettingsService {

    private final DocumentSettingsRepo repo;
    private final OrgRepository orgRepo;
    private final ObjectMapper om = new ObjectMapper();

    public DocumentSettingsService(DocumentSettingsRepo repo, OrgRepository orgRepo) {
        this.repo = repo;
        this.orgRepo = orgRepo;
    }

    /** Get settings for an org (with defaults if none exist) */
    @Transactional(readOnly = true)
    public DocumentSettingsDto get(Long orgId) {
        if (!orgRepo.existsById(orgId)) {
            throw new IllegalArgumentException("OrgId " + orgId + " does not exist.");
        }
        return repo.findByOrgId(orgId)
                .map(this::toDto)
                .orElse(defaultsFor(orgId));
    }

    /** Save or update settings */
    @Transactional
    public DocumentSettingsDto save(DocumentSettingsDto dto, String user) {
        if (!orgRepo.existsById(dto.getOrgId())) {
            throw new IllegalArgumentException("OrgId " + dto.getOrgId() + " does not exist.");
        }

        DocumentSettings s = repo.findByOrgId(dto.getOrgId())
                .orElse(new DocumentSettings());

        s.setOrgId(dto.getOrgId());
        s.setMaxUploadBytes(Math.max(1, dto.getMaxUploadSizeMB()) * 1024L * 1024L);
        s.setEnableAudio(dto.isEnableAudio());
        s.setEncryptionEnabled(dto.isEncryptionEnabled()); // ✅ NEW: persist encryption flag

        // Always clean MP3 depending on audio toggle
        List<String> types = dto.getAllowedFileTypes() != null
                ? new ArrayList<>(dto.getAllowedFileTypes())
                : new ArrayList<>();

        if (s.isEnableAudio()) {
            if (!types.contains("MP3")) {
                types.add("MP3");
            }
        } else {
            types.remove("MP3");
        }

        s.setAllowedFileTypesJson(writeJson(types));
        s.setCategoriesJson(writeJson(dto.getCategories()));
        s.setUpdatedBy(user);
        s.setUpdatedAt(Instant.now());

        repo.save(s);
        return toDto(s);
    }

    /** Get categories only */
    @Transactional(readOnly = true)
    public List<DocumentSettingsDto.Category> getCategories(Long orgId) {
        if (!orgRepo.existsById(orgId)) {
            throw new IllegalArgumentException("OrgId " + orgId + " does not exist.");
        }
        DocumentSettings s = repo.findByOrgId(orgId).orElseThrow();
        return parseList(s.getCategoriesJson(), new TypeReference<>() {});
    }

    /** Add category */
    @Transactional
    public List<DocumentSettingsDto.Category> addCategory(Long orgId, String name, boolean active, String user) {
        if (!orgRepo.existsById(orgId)) {
            throw new IllegalArgumentException("OrgId " + orgId + " does not exist.");
        }

        DocumentSettings s = repo.findByOrgId(orgId).orElseThrow();
        var categories = parseList(s.getCategoriesJson(),
                new TypeReference<List<DocumentSettingsDto.Category>>() {});

        boolean exists = categories.stream().anyMatch(c -> c.getName().equalsIgnoreCase(name));
        if (!exists) {
            categories.add(new DocumentSettingsDto.Category(name, active));
            s.setCategoriesJson(writeJson(categories));
            s.setUpdatedBy(user);
            s.setUpdatedAt(Instant.now());
            repo.save(s);
        }
        return categories;
    }

    // ---------- helpers ----------

    private DocumentSettingsDto defaultsFor(Long orgId) {
        return DocumentSettingsDto.builder()
                .orgId(orgId)
                .maxUploadSizeMB(50)      // default 50 MB
                .enableAudio(false)       // default false
                .encryptionEnabled(false) // ✅ NEW: default encryption off
                .allowedFileTypes(List.of()) // empty until user selects
                .categories(List.of())       // empty until user adds
                .build();
    }

    private DocumentSettingsDto toDto(DocumentSettings s) {
        int mb = (int) Math.max(1, Math.round(s.getMaxUploadBytes() / 1024.0 / 1024.0));
        var types = parseList(s.getAllowedFileTypesJson(), new TypeReference<List<String>>() {});

        // Normalize MP3 based on enableAudio
        if (s.isEnableAudio() && !types.contains("MP3")) {
            types = new ArrayList<>(types);
            types.add("MP3");
        }
        if (!s.isEnableAudio() && types.contains("MP3")) {
            types = new ArrayList<>(types);
            types.remove("MP3");
        }

        var cats  = parseList(s.getCategoriesJson(), new TypeReference<List<DocumentSettingsDto.Category>>() {});
        return DocumentSettingsDto.builder()
                .orgId(s.getOrgId())
                .maxUploadSizeMB(mb)
                .enableAudio(s.isEnableAudio())
                .encryptionEnabled(s.isEncryptionEnabled()) // ✅ NEW: return encryption flag
                .allowedFileTypes(types)
                .categories(cats)
                .build();
    }

    private <T> List<T> parseList(String json, TypeReference<List<T>> type) {
        try { return om.readValue(Optional.ofNullable(json).orElse("[]"), type); }
        catch (Exception e) { return List.of(); }
    }

    private String writeJson(Object value) {
        try { return om.writeValueAsString(value == null ? List.of() : value); }
        catch (Exception e) { return "[]"; }
    }
}
