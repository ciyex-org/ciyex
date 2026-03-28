package org.ciyex.ehr.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.inventory.dto.InvSupplierDto;
import org.ciyex.ehr.inventory.entity.InvSupplier;
import org.ciyex.ehr.inventory.repository.InvSupplierRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvSupplierService2 {

    private final InvSupplierRepository repo;

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    @Transactional
    public InvSupplierDto create(InvSupplierDto dto) {
        var entity = InvSupplier.builder()
                .name(dto.getName())
                .contactName(dto.getContactName())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .address(dto.getAddress())
                .notes(dto.getNotes())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .orgAlias(orgAlias())
                .build();
        return toDto(repo.save(entity));
    }

    @Transactional(readOnly = true)
    public InvSupplierDto getById(Long id) {
        return repo.findById(id)
                .filter(s -> s.getOrgAlias().equals(orgAlias()))
                .map(this::toDto)
                .orElseThrow(() -> new NoSuchElementException("Supplier not found: " + id));
    }

    @Transactional
    public InvSupplierDto update(Long id, InvSupplierDto dto) {
        var entity = repo.findById(id)
                .filter(s -> s.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Supplier not found: " + id));

        entity.setName(dto.getName());
        entity.setContactName(dto.getContactName());
        entity.setPhone(dto.getPhone());
        entity.setEmail(dto.getEmail());
        entity.setAddress(dto.getAddress());
        entity.setNotes(dto.getNotes());
        if (dto.getActive() != null) entity.setActive(dto.getActive());

        return toDto(repo.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        var entity = repo.findById(id)
                .filter(s -> s.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Supplier not found: " + id));
        repo.delete(entity);
    }

    @Transactional(readOnly = true)
    public Page<InvSupplierDto> getAll(Pageable pageable) {
        return repo.findByOrgAlias(orgAlias(), pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<InvSupplierDto> getAll() {
        return repo.findByOrgAlias(orgAlias()).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public long count() {
        return repo.countByOrgAlias(orgAlias());
    }

    private InvSupplierDto toDto(InvSupplier e) {
        return InvSupplierDto.builder()
                .id(e.getId())
                .name(e.getName())
                .contactName(e.getContactName())
                .phone(e.getPhone())
                .email(e.getEmail())
                .address(e.getAddress())
                .notes(e.getNotes())
                .active(e.getActive())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }
}
