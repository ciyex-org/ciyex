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

    /** Set the PostgreSQL schema for this transaction using the orgId. */
    private void setSearchPath(Long orgId) {
        if (orgId == null) throw new IllegalArgumentException("orgId cannot be null");
        // practice_{orgId} must exist (you already provision it).
        em.createNativeQuery("set local search_path to practice_" + orgId).executeUpdate();
    }

    @Transactional(readOnly = true)
    public List<PatientCodeListDto> findAll(Long orgId) {
        setSearchPath(orgId);
        return repo.findAllByOrgIdOrderByOrderIndexAsc(orgId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PatientCodeListDto getById(Long orgId, Long id) {
        setSearchPath(orgId);
        Optional<PatientCodeList> opt = repo.findByIdAndOrgId(id, orgId);
        return opt.map(this::toDto).orElse(null);
    }

    @Transactional
    public PatientCodeListDto create(Long orgId, PatientCodeListDto dto) {
        setSearchPath(orgId);
        PatientCodeList entity = fromDto(orgId, dto, new PatientCodeList());
        PatientCodeList saved = repo.save(entity);
        if (saved.isDefault()) {
            repo.clearDefaultsExcept(orgId, saved.getId());
        }
        return toDto(saved);
    }

    @Transactional
    public PatientCodeListDto update(Long orgId, Long id, PatientCodeListDto dto) {
        setSearchPath(orgId);
        PatientCodeList entity = repo.findByIdAndOrgId(id, orgId).orElse(null);
        if (entity == null) return null;

        entity = fromDto(orgId, dto, entity);
        PatientCodeList saved = repo.save(entity);
        if (saved.isDefault()) {
            repo.clearDefaultsExcept(orgId, saved.getId());
        }
        return toDto(saved);
    }

    @Transactional
    public boolean delete(Long orgId, Long id) {
        setSearchPath(orgId);
        PatientCodeList entity = repo.findByIdAndOrgId(id, orgId).orElse(null);
        if (entity == null) return false;
        repo.deleteByIdAndOrgId(id, orgId);
        return true;
    }

    @Transactional
    public List<PatientCodeListDto> saveBulk(Long orgId, List<PatientCodeListDto> rows) {
        setSearchPath(orgId);

        // Enforce orgId & single default (keep first true)
        boolean seenDefault = false;
        for (PatientCodeListDto r : rows) {
            r.orgId = orgId;
            if (r.isDefault) {
                if (seenDefault) r.isDefault = false;
                else seenDefault = true;
            }
        }

        List<PatientCodeList> entities = rows.stream()
                .map(r -> {
                    PatientCodeList e = (r.id != null)
                            ? repo.findByIdAndOrgId(r.id, orgId).orElse(new PatientCodeList())
                            : new PatientCodeList();
                    return fromDto(orgId, r, e);
                })
                .sorted(Comparator.comparing(PatientCodeList::getOrderIndex))
                .collect(Collectors.toList());

        List<PatientCodeList> saved = repo.saveAll(entities);

        Long keepId = saved.stream().filter(PatientCodeList::isDefault)
                .findFirst().map(PatientCodeList::getId).orElse(null);
        if (keepId == null) repo.clearAllDefaults(orgId);
        else repo.clearDefaultsExcept(orgId, keepId);

        return saved.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public PatientCodeListDto setDefault(Long orgId, Long id) {
        setSearchPath(orgId);
        PatientCodeList e = repo.findByIdAndOrgId(id, orgId).orElse(null);
        if (e == null) return null;
        e.setDefault(true);
        repo.save(e);
        repo.clearDefaultsExcept(orgId, e.getId());
        return toDto(e);
    }

    // ---- mapping helpers ----

    private PatientCodeList fromDto(Long orgId, PatientCodeListDto dto, PatientCodeList e) {
        e.setOrgId(orgId);
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
        d.orgId = e.getOrgId();
        d.title = e.getTitle();
        d.order = e.getOrderIndex();
        d.isDefault = e.isDefault();
        d.active = e.isActive();
        d.notes = e.getNotes();
        d.codes = e.getCodes();
        return d;
    }
}
