package org.ciyex.ehr.education.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.education.dto.EducationMaterialDto;
import org.ciyex.ehr.education.entity.EducationMaterial;
import org.ciyex.ehr.education.repository.EducationMaterialRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EducationMaterialService {

    private final EducationMaterialRepository repo;

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    @Transactional
    public EducationMaterialDto create(EducationMaterialDto dto) {
        var material = EducationMaterial.builder()
                .title(dto.getTitle())
                .category(dto.getCategory())
                .contentType(dto.getContentType() != null ? dto.getContentType() : "article")
                .content(dto.getContent())
                .externalUrl(dto.getExternalUrl())
                .language(dto.getLanguage() != null ? dto.getLanguage() : "en")
                .audience(dto.getAudience() != null ? dto.getAudience() : "patient")
                .tags(dto.getTags())
                .author(dto.getAuthor())
                .source(dto.getSource())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .viewCount(0)
                .orgAlias(orgAlias())
                .build();
        material = repo.save(material);
        return toDto(material);
    }

    @Transactional(readOnly = true)
    public EducationMaterialDto getById(Long id) {
        return repo.findById(id)
                .filter(m -> m.getOrgAlias().equals(orgAlias()))
                .map(this::toDto)
                .orElseThrow(() -> new NoSuchElementException("Material not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<EducationMaterialDto> getAll(Pageable pageable) {
        return repo.findByOrgAliasOrderByCreatedAtDesc(orgAlias(), pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<EducationMaterialDto> search(String query) {
        if (query == null || query.isBlank()) {
            return repo.findByOrgAliasAndIsActiveTrueOrderByTitleAsc(orgAlias())
                    .stream().map(this::toDto).toList();
        }
        return repo.search(orgAlias(), query.trim())
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<EducationMaterialDto> getByCategory(String category) {
        return repo.findByOrgAliasAndCategoryOrderByTitleAsc(orgAlias(), category)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public EducationMaterialDto update(Long id, EducationMaterialDto dto) {
        var material = repo.findById(id)
                .filter(m -> m.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Material not found: " + id));

        if (dto.getTitle() != null) material.setTitle(dto.getTitle());
        if (dto.getCategory() != null) material.setCategory(dto.getCategory());
        if (dto.getContentType() != null) material.setContentType(dto.getContentType());
        if (dto.getContent() != null) material.setContent(dto.getContent());
        if (dto.getExternalUrl() != null) material.setExternalUrl(dto.getExternalUrl());
        if (dto.getLanguage() != null) material.setLanguage(dto.getLanguage());
        if (dto.getAudience() != null) material.setAudience(dto.getAudience());
        if (dto.getTags() != null) material.setTags(dto.getTags());
        if (dto.getAuthor() != null) material.setAuthor(dto.getAuthor());
        if (dto.getSource() != null) material.setSource(dto.getSource());
        if (dto.getIsActive() != null) material.setIsActive(dto.getIsActive());

        return toDto(repo.save(material));
    }

    @Transactional
    public void delete(Long id) {
        var material = repo.findById(id)
                .filter(m -> m.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Material not found: " + id));
        repo.delete(material);
    }

    @Transactional
    public void incrementViewCount(Long id) {
        var material = repo.findById(id)
                .filter(m -> m.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Material not found: " + id));
        material.setViewCount(material.getViewCount() != null ? material.getViewCount() + 1 : 1);
        repo.save(material);
    }

    private EducationMaterialDto toDto(EducationMaterial e) {
        return EducationMaterialDto.builder()
                .id(e.getId())
                .title(e.getTitle())
                .category(e.getCategory())
                .contentType(e.getContentType())
                .content(e.getContent())
                .externalUrl(e.getExternalUrl())
                .language(e.getLanguage())
                .audience(e.getAudience())
                .tags(e.getTags())
                .author(e.getAuthor())
                .source(e.getSource())
                .isActive(e.getIsActive())
                .viewCount(e.getViewCount())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }
}
