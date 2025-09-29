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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

        AdminTemplate entity = mapToEntity(dto);
        entity = repository.save(entity);

        dto.setId(entity.getId());
        // templateId removed — id is the primary identifier
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

    /**
     * Return a paginated page of AdminTemplateDto. Page argument is 1-based (clients pass 1 for first page).
     */
    @Transactional(readOnly = true)
    public Page<AdminTemplateDto> getPaginated(int page, int size) {
        Long orgId = getCurrentOrgId();
        if (page < 1) page = 1;
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<AdminTemplate> p = repository.findAllByOrgId(orgId, pageable);
        List<AdminTemplateDto> dtos = p.stream().map(this::mapToDto).collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, p.getTotalElements());
    }

    // --- Helpers ---

    private AdminTemplate mapToEntity(AdminTemplateDto dto) {
        return AdminTemplate.builder()
                .id(dto.getId())
                .orgId(dto.getOrgId())
                .locations(dto.getLocations())
                .practiceType(dto.getPracticeType())
                .build();
    }

    private AdminTemplateDto mapToDto(AdminTemplate entity) {
        AdminTemplateDto dto = new AdminTemplateDto();
        dto.setId(entity.getId());
        dto.setOrgId(entity.getOrgId());
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

    // templateId generation removed
}
