package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.TemplateDto;
import com.qiaben.ciyex.entity.Template;
import com.qiaben.ciyex.repository.TemplateRepository;
import com.qiaben.ciyex.dto.integration.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TemplateService {

    private final TemplateRepository repository;

    public TemplateService(TemplateRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TemplateDto create(TemplateDto dto) {
        
        Template entity = mapToEntity(dto);
        entity = repository.save(entity);

        dto.setId(entity.getId());
        dto.setExternalId(entity.getExternalId());
        dto.setAudit(toAudit(entity));
        dto.setTenantName(RequestContext.get() != null ? RequestContext.get().getTenantName() : null);
        return dto;
    }

    @Transactional(readOnly = true)
    public TemplateDto getById(Long id) {
        
        Template entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));
    return mapToDto(entity);
    }

    @Transactional
    public TemplateDto update(Long id, TemplateDto dto) {
        
        Template entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        if (dto.getTemplateName() != null) entity.setTemplateName(dto.getTemplateName());
        if (dto.getSubject() != null) entity.setSubject(dto.getSubject());
        if (dto.getBody() != null) entity.setBody(dto.getBody());

        entity = repository.save(entity);
    return mapToDto(entity);
    }

    @Transactional
    public void delete(Long id) {
        
        Template entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        repository.delete(entity);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<TemplateDto>> getAllTemplates() {
        List<Template> templates = repository.findAll();
        List<TemplateDto> dtos = templates.stream().map(this::mapToDto).collect(Collectors.toList());

        return ApiResponse.<List<TemplateDto>>builder()
                .success(true)
                .message("Templates retrieved successfully")
                .data(dtos)
                .build();
    }

    private Template mapToEntity(TemplateDto dto) {
        return Template.builder()
                .id(dto.getId())
                .externalId(dto.getExternalId())
                .templateName(dto.getTemplateName())
                .subject(dto.getSubject())
                .body(dto.getBody())
                .build();
    }

    private TemplateDto mapToDto(Template entity) {
        TemplateDto dto = new TemplateDto();
        dto.setId(entity.getId());
        dto.setExternalId(entity.getExternalId());
        dto.setTemplateName(entity.getTemplateName());
        dto.setSubject(entity.getSubject());
        dto.setBody(entity.getBody());
        dto.setAudit(toAudit(entity));
        dto.setTenantName(RequestContext.get() != null ? RequestContext.get().getTenantName() : null);
        return dto;
    }

    private TemplateDto.Audit toAudit(Template entity) {
        TemplateDto.Audit audit = new TemplateDto.Audit();
        audit.setCreatedAt(entity.getCreatedAt());
        audit.setUpdatedAt(entity.getUpdatedAt());
        return audit;
    }
}
