package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.AdminTemplateDto;
import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.entity.AdminTemplate;
import com.qiaben.ciyex.repository.AdminTemplateRepository;
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
        // Validate mandatory fields
        validateMandatoryFields(dto);
        
        AdminTemplate entity = mapToEntity(dto);
        entity = repository.save(entity);

        dto.setId(entity.getId());
        // templateId removed — id is the primary identifier
        dto.setAudit(toAudit(entity));
        return dto;
    }

    @Transactional(readOnly = true)
    public AdminTemplateDto getById(Long id) {
        AdminTemplate entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("AdminTemplate not found"));
        return mapToDto(entity);
    }

    @Transactional
    public AdminTemplateDto update(Long id, AdminTemplateDto dto) {
        AdminTemplate entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("AdminTemplate not found"));

        if (dto.getLocations() != null) {
            entity.setLocations(dto.getLocations());
        }
        if (dto.getPracticeType() != null) {
            entity.setPracticeType(dto.getPracticeType());
        }

        // Validate mandatory fields after update
        validateMandatoryFields(entity);

        entity = repository.save(entity);
        return mapToDto(entity);
    }

    @Transactional
    public void delete(Long id) {
        AdminTemplate entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("AdminTemplate not found"));
        repository.delete(entity);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<AdminTemplateDto>> getAll() {
        List<AdminTemplate> list = repository.findAll();
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
        if (page < 1) page = 1;
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<AdminTemplate> p = repository.findAll(pageable);
        List<AdminTemplateDto> dtos = p.stream().map(this::mapToDto).collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, p.getTotalElements());
    }

    // --- Helpers ---

    private AdminTemplate mapToEntity(AdminTemplateDto dto) {
        return AdminTemplate.builder()
                .id(dto.getId())
                .locations(dto.getLocations())
                .practiceType(dto.getPracticeType())
                .build();
    }

    private AdminTemplateDto mapToDto(AdminTemplate entity) {
        AdminTemplateDto dto = new AdminTemplateDto();
        dto.setId(entity.getId());
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

    // RequestContext/org scoping removed for AdminTemplate service

    // templateId generation removed

    // ---- Validation helpers ----
    private void validateMandatoryFields(AdminTemplateDto dto) {
        if (dto == null) throw new IllegalArgumentException("admin template payload is required");
        if (isBlank(dto.getLocations())) throw new IllegalArgumentException("locations is required");
        if (isBlank(dto.getPracticeType())) throw new IllegalArgumentException("practiceType is required");
    }

    private void validateMandatoryFields(AdminTemplate entity) {
        if (entity == null) throw new IllegalArgumentException("admin template is required");
        if (isBlank(entity.getLocations())) throw new IllegalArgumentException("locations is required");
        if (isBlank(entity.getPracticeType())) throw new IllegalArgumentException("practiceType is required");
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
