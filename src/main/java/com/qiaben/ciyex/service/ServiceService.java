package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ServicebillDto;
import com.qiaben.ciyex.entity.ServiceEntity;
import com.qiaben.ciyex.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final ServiceRepository repository;

    @Transactional
    public ServicebillDto create(ServicebillDto dto) {
        if (repository.existsByName(dto.getName())) {
            throw new RuntimeException("Service already exists: " + dto.getName());
        }
        ServiceEntity entity = mapToEntity(dto);
        String now = now();
        entity.setCreatedDate(now);
        entity.setLastModifiedDate(now);
        return mapToDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public ServicebillDto getById(Long id) {
        return repository.findById(id).map(this::mapToDto)
                .orElseThrow(() -> new RuntimeException("Service not found with id " + id));
    }

    @Transactional(readOnly = true)
    public List<ServicebillDto> getAll() {
        return repository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public ServicebillDto update(Long id, ServicebillDto dto) {
        ServiceEntity entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found with id " + id));
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getDefaultPrice() != null) entity.setDefaultPrice(dto.getDefaultPrice());
        entity.setLastModifiedDate(now());
        return mapToDto(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        ServiceEntity entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found with id " + id));
        repository.delete(entity);
    }

    private ServicebillDto mapToDto(ServiceEntity entity) {
        ServicebillDto dto = new ServicebillDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDefaultPrice(entity.getDefaultPrice());

        ServicebillDto.Audit audit = new ServicebillDto.Audit();
        audit.setCreatedDate(entity.getCreatedDate());
        audit.setLastModifiedDate(entity.getLastModifiedDate());
        dto.setAudit(audit);

        return dto;
    }

    private ServiceEntity mapToEntity(ServicebillDto dto) {
        return ServiceEntity.builder()
                .name(dto.getName())
                .defaultPrice(dto.getDefaultPrice())
                .build();
    }

    private String now() {
        return LocalDateTime.now().format(FORMATTER);
    }
}
