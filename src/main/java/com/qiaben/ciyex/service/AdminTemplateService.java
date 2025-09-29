package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.AdminTemplateDto;
import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.entity.AdminTemplate;
import com.qiaben.ciyex.repository.AdminTemplateRepository;
import com.qiaben.ciyex.dto.integration.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AdminTemplateService {

    private final AdminTemplateRepository repository;

    public AdminTemplateService(AdminTemplateRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AdminTemplateDto create(AdminTemplateDto dto) {
        Long orgId = getCurrentOrgId();
        if (orgId == null) throw new SecurityException("No orgId in RequestContext");
        dto.setOrgId(orgId);

        // Auto-generate a templateId if missing
        if (dto.getTemplateId() == null || dto.getTemplateId().trim().isEmpty()) {
            dto.setTemplateId(generateUniqueTemplateId());
        }

        AdminTemplate entity = mapToEntity(dto);
        entity = repository.save(entity);

        dto.setId(entity.getId());
        dto.setTemplateId(entity.getTemplateId());
        dto.setAudit(toAudit(entity));
        return dto;
    }

    @Transactional(readOnly = true)
    public AdminTemplateDto getById(Long id) {
        Long orgId = getCurrentOrgId();
        AdminTemplate entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("AdminTemplate not found"));
        if (!entity.getOrgId().equals(orgId)) {
            throw new SecurityException("Access denied");
        }
        return mapToDto(entity);
    }

    @Transactional
    public AdminTemplateDto update(Long id, AdminTemplateDto dto) {
        Long orgId = getCurrentOrgId();
        AdminTemplate entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("AdminTemplate not found"));
        if (!entity.getOrgId().equals(orgId)) {
            throw new SecurityException("Access denied");
        }

        if (dto.getLocations() != null) {
            entity.setLocations(dto.getLocations());
        }
        if (dto.getPracticeType() != null) {
            entity.setPracticeType(dto.getPracticeType());
        }

        entity = repository.save(entity);
        return mapToDto(entity);
    }

    @Transactional
    public void delete(Long id) {
        Long orgId = getCurrentOrgId();
        AdminTemplate entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("AdminTemplate not found"));
        if (!entity.getOrgId().equals(orgId)) {
            throw new SecurityException("Access denied");
        }
        repository.delete(entity);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<AdminTemplateDto>> getAll() {
        Long orgId = getCurrentOrgId();
        List<AdminTemplate> list = repository.findAllByOrgId(orgId);
        List<AdminTemplateDto> dtos = list.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return ApiResponse.<List<AdminTemplateDto>>builder()
                .success(true)
                .message("Admin templates retrieved successfully")
                .data(dtos)
                .build();
    }

    // --- Helpers ---

    private AdminTemplate mapToEntity(AdminTemplateDto dto) {
        return AdminTemplate.builder()
                .id(dto.getId())
                .orgId(dto.getOrgId())
                .templateId(dto.getTemplateId())
                .locations(dto.getLocations())
                .practiceType(dto.getPracticeType())
                .build();
    }

    private AdminTemplateDto mapToDto(AdminTemplate entity) {
        AdminTemplateDto dto = new AdminTemplateDto();
        dto.setId(entity.getId());
        dto.setOrgId(entity.getOrgId());
        dto.setTemplateId(entity.getTemplateId());
        dto.setLocations(entity.getLocations());
        dto.setPracticeType(entity.getPracticeType());
        dto.setAudit(toAudit(entity));
        return dto;
    }

    private AdminTemplateDto.Audit toAudit(AdminTemplate entity) {
        AdminTemplateDto.Audit audit = new AdminTemplateDto.Audit();
        audit.setCreatedAt(entity.getCreatedAt());
        audit.setUpdatedAt(entity.getUpdatedAt());
        return audit;
    }

    private Long getCurrentOrgId() {
        return RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
    }

    private String generateUniqueTemplateId() {
        String candidate;
        int attempts = 0;
        do {
            int n = (int) (Math.random() * 9000) + 1000;
            candidate = "TPL-" + n;
            attempts++;
            if (attempts > 100) {
                throw new RuntimeException("Unable to generate unique template id");
            }
        } while (repository.existsByTemplateId(candidate));
        return candidate;
    }
}
