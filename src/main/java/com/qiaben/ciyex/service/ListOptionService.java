package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ListOptionDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.entity.ListOption;
import com.qiaben.ciyex.repository.ListOptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ListOptionService {

    private final ListOptionRepository repository;

    @Autowired
    public ListOptionService(ListOptionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ListOptionDto create(ListOptionDto dto) {
        ListOption entity = new ListOption();
        entity.setOrgId(RequestContext.get().getTenantName());
        entity.setListId(dto.getListId());
        entity.setOptionId(dto.getOptionId());
        entity.setTitle(dto.getTitle());
        entity.setSeq(dto.getSeq());
        entity.setIsDefault(dto.getIsDefault());
        entity.setOptionValue(dto.getOptionValue());
        entity.setNotes(dto.getNotes());
        entity.setCodes(dto.getCodes());
        entity.setActivity(dto.getActivity());
        entity.setEditOptions(dto.getEditOptions());
        entity.setTimestamp(LocalDateTime.now());
        entity.setLastUpdated(LocalDateTime.now());

        ListOption savedEntity = repository.save(entity);

        return convertToDto(savedEntity);
    }

    @Transactional
    public ListOptionDto update(Long id, ListOptionDto dto) {
        ListOption existing = repository.findById(id).orElseThrow(() -> new RuntimeException("Option not found"));

        existing.setTitle(dto.getTitle());
        existing.setSeq(dto.getSeq());
        existing.setIsDefault(dto.getIsDefault());
        existing.setOptionValue(dto.getOptionValue());
        existing.setNotes(dto.getNotes());
        existing.setCodes(dto.getCodes());
        existing.setActivity(dto.getActivity());
        existing.setEditOptions(dto.getEditOptions());
        existing.setLastUpdated(LocalDateTime.now());

        ListOption updatedEntity = repository.save(existing);
        return convertToDto(updatedEntity);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    public ListOptionDto get(Long id) {
        ListOption entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Option not found"));
        return convertToDto(entity);
    }

    public List<ListOptionDto> getAll() {
        List<ListOption> entities = repository.findAll();
        return entities.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    private ListOptionDto convertToDto(ListOption entity) {
        ListOptionDto dto = new ListOptionDto();
        dto.setId(entity.getId());
        dto.setOrgId(entity.getOrgId());
        dto.setListId(entity.getListId());
        dto.setOptionId(entity.getOptionId());
        dto.setTitle(entity.getTitle());
        dto.setSeq(entity.getSeq());
        dto.setIsDefault(entity.getIsDefault());
        dto.setOptionValue(entity.getOptionValue());
        dto.setNotes(entity.getNotes());
        dto.setCodes(entity.getCodes());
        dto.setActivity(entity.getActivity());
        dto.setEditOptions(entity.getEditOptions());
        dto.setTimestamp(entity.getTimestamp());
        dto.setLastUpdated(entity.getLastUpdated());

        return dto;
    }

    // Service method to fetch list options based on list_id
    public List<ListOptionDto> getListOptionsByListId(String listId) {
        
        RequestContext ctx = RequestContext.get();
        String tenantName = ctx != null ? ctx.getTenantName() : null;
        if (tenantName == null || tenantName.isBlank()) {
            throw new IllegalStateException("No tenantName found in request context");
        }
        
        List<ListOption> listOptions = repository.findByListId(listId);
        return listOptions.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Transactional
    public void deleteByListId(String listId) {
        List<ListOption> listOptions = repository.findByListId(listId);
        if (!listOptions.isEmpty()) {
            repository.deleteByListId(listId);  // Custom delete method based on list_id
        }
    }
}
