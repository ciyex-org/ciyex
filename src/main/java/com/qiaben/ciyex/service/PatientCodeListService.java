package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.PatientCodeListDto;
import com.qiaben.ciyex.entity.PatientCodeList;
import com.qiaben.ciyex.repository.PatientCodeListRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientCodeListService {

    private final PatientCodeListRepository repo;

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public List<PatientCodeListDto> findAll() {
        return repo.findAll()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PatientCodeListDto getById(Long id) {
        Optional<PatientCodeList> opt = repo.findById(id);
        return opt.map(this::toDto).orElse(null);
    }

    @Transactional
    public PatientCodeListDto create(PatientCodeListDto dto) {
        PatientCodeList entity = fromDto(dto, new PatientCodeList());
        PatientCodeList saved = repo.save(entity);
        if (saved.isDefault()) {
            repo.clearDefaultsExcept(saved.getId());
        }
        return toDto(saved);
    }

    @Transactional
    public PatientCodeListDto update(Long id, PatientCodeListDto dto) {
        PatientCodeList entity = repo.findById(id).orElse(null);
        if (entity == null) return null;

        entity = fromDto(dto, entity);
        PatientCodeList saved = repo.save(entity);
        if (saved.isDefault()) {
            repo.clearDefaultsExcept(saved.getId());
        }
        return toDto(saved);
    }

    @Transactional
    public boolean delete(Long id) {
        PatientCodeList entity = repo.findById(id).orElse(null);
        if (entity == null) return false;
        repo.deleteById(id);
        return true;
    }

    @Transactional
    public List<PatientCodeListDto> saveBulk(List<PatientCodeListDto> rows) {

        
        boolean seenDefault = false;
        for (PatientCodeListDto r : rows) {
            if (r.isDefault) {
                if (seenDefault) r.isDefault = false;
                else seenDefault = true;
            }
        }

        List<PatientCodeList> entities = rows.stream()
                .map(r -> {
                    PatientCodeList e = (r.id != null)
                            ? repo.findById(r.id).orElse(new PatientCodeList())
                            : new PatientCodeList();
                    return fromDto(r, e);
                })
                .sorted(Comparator.comparing(PatientCodeList::getOrderIndex))
                .collect(Collectors.toList());

        List<PatientCodeList> saved = repo.saveAll(entities);

        Long keepId = saved.stream().filter(PatientCodeList::isDefault)
                .findFirst().map(PatientCodeList::getId).orElse(null);
        if (keepId == null) repo.clearAllDefaults();
        else repo.clearDefaultsExcept(keepId);

        return saved.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public PatientCodeListDto setDefault(Long id) {
        PatientCodeList e = repo.findById(id).orElse(null);
        if (e == null) return null;
        e.setDefault(true);
        repo.save(e);
        repo.clearDefaultsExcept(e.getId());
        return toDto(e);
    }

    // ---- mapping helpers ----

    private PatientCodeList fromDto(PatientCodeListDto dto, PatientCodeList e) {
        if (dto.title != null) e.setTitle(dto.title);
        if (dto.order != null) e.setOrderIndex(dto.order);
        e.setDefault(dto.isDefault);
        e.setActive(dto.active);
        e.setNotes(dto.notes);
        e.setCodes(dto.codes);
        return e;
    }

    private PatientCodeListDto toDto(PatientCodeList e) {
        PatientCodeListDto d = new PatientCodeListDto();
        d.id = e.getId();
        d.title = e.getTitle();
        d.order = e.getOrderIndex();
        d.isDefault = e.isDefault();
        d.active = e.isActive();
        d.notes = e.getNotes();
        d.codes = e.getCodes();
        return d;
    }
}
